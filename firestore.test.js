const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_host_123";
const BOB_UID = "bob_viewer_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfilePayload(uid, now) {
  return {
    userId: uid,
    displayName: "Aria Spark",
    bio: "Acoustic singer & PK streamer",
    coinBalance: 2500,
    diamondsEarned: 0,
    level: 5,
    followingCount: 12,
    followersCount: 48,
    dailyCheckInDay: 1,
    createdAt: now,
    updatedAt: now,
  };
}

function validStreamPayload(hostId, now, overrides = {}) {
  return {
    hostId,
    hostName: "Aria Spark",
    hostLevel: 12,
    title: "Late Night Acoustic Jam",
    category: "MUSIC",
    coverKey: "acoustic",
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    visibility: "PUBLIC",
    status: "LIVE",
    pkActive: false,
    partyMode: false,
    pkHostScore: 0,
    pkOpponentScore: 0,
    viewerCount: 1,
    totalGiftsCoins: 0,
    likeCount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: denied from reading or writing any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("live_streams").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("live_streams").doc("stream_1").set(validStreamPayload(ALICE_UID, now)));
});

test("UserProfile, Transactions & Following: strict owner-only access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now))
  );

  // Follow host under owner's subcollection
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("following").doc(ALICE_UID).set({
      userId: BOB_UID,
      hostId: ALICE_UID,
      hostName: "Aria Spark",
      hostLevel: 12,
      createdAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("following").get()
  );
  // Cross-user following read/write rejected
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("following").get()
  );

  // Reject ghost field & cross-user profile update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("LiveStreams & PartySeats: public query succeeds, unfiltered fails, seat claim/mute/leave works", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("live_streams").doc("stream_party_1").set(
      validStreamPayload(ALICE_UID, created, { partyMode: true })
    )
  );

  // Viewer claims seat 2
  await assertSucceeds(
    bobDb.collection("live_streams").doc("stream_party_1").collection("seats").doc("seat_2").set({
      streamId: "stream_party_1",
      streamVisibility: "PUBLIC",
      hostId: ALICE_UID,
      seatNumber: 2,
      occupantId: BOB_UID,
      occupantName: "Bob",
      occupantLevel: 4,
      micMuted: false,
      createdAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Filtered query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("live_streams").doc("stream_party_1").collection("seats")
      .where("streamVisibility", "==", "PUBLIC").get()
  );
  await assertFails(
    bobDb.collection("live_streams").doc("stream_party_1").collection("seats").get()
  );

  // Host or occupant can mute mic; occupant can leave seat
  await assertSucceeds(
    aliceDb.collection("live_streams").doc("stream_party_1").collection("seats").doc("seat_2").update({
      micMuted: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("live_streams").doc("stream_party_1").collection("seats").doc("seat_2").delete()
  );
});

test("StreamMessages: real-time chat & gift events require parent stream and filtered query", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("live_streams").doc("stream_chat_1").set(validStreamPayload(ALICE_UID, now))
  );

  await assertSucceeds(
    bobDb.collection("live_streams").doc("stream_chat_1").collection("messages").doc("msg_1").set({
      streamId: "stream_chat_1",
      streamVisibility: "PUBLIC",
      hostId: ALICE_UID,
      senderId: BOB_UID,
      senderName: "Bob",
      senderLevel: 3,
      messageType: "GIFT",
      body: "Sent Galaxy Rocket x1!",
      giftName: "Galaxy Rocket",
      giftCoins: 199,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    bobDb.collection("live_streams").doc("stream_chat_1").collection("messages").get()
  );
});
