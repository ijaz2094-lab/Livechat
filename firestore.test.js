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

test("UserProfile & Transactions: strict owner-only access and shadow-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now))
  );

  // Partial update on profile metadata
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Aria Live",
      bio: "Live every night!",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field & cross-user access
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ coinBalance: 999999, updatedAt: new Date(Date.now() - 500) })
  );

  // Owner transaction create succeeds; immutable update fails
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      txType: "DAILY_BONUS",
      title: "Daily Check-in Day 1",
      coinDelta: 500,
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({ coinDelta: 9999 })
  );
});

test("LiveStreams: public query succeeds, unfiltered query fails, terminal lock & engagement rules hold", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Create PUBLIC LIVE stream without @DocumentId id field
  await assertSucceeds(
    aliceDb.collection("live_streams").doc("stream_1").set(validStreamPayload(ALICE_UID, created))
  );

  // Reject @DocumentId `id` key in payload & future timestamp
  await assertFails(
    aliceDb.collection("live_streams").doc("stream_bad").set({
      id: "stream_bad",
      ...validStreamPayload(ALICE_UID, created),
    })
  );
  await assertFails(
    aliceDb.collection("live_streams").doc("stream_future").set(
      validStreamPayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  // Filtered PUBLIC query succeeds; unfiltered list fails
  await assertSucceeds(
    bobDb.collection("live_streams").where("visibility", "==", "PUBLIC").get()
  );
  await assertFails(bobDb.collection("live_streams").get());

  // Viewer engagement update succeeds on counter fields only
  await assertSucceeds(
    bobDb.collection("live_streams").doc("stream_1").update({
      likeCount: 5,
      totalGiftsCoins: 100,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Viewer cannot hijack title or status
  await assertFails(
    bobDb.collection("live_streams").doc("stream_1").update({
      title: "Hacked Title",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Host ends stream -> terminal state locks subsequent updates
  await assertSucceeds(
    aliceDb.collection("live_streams").doc("stream_1").update({
      status: "ENDED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("live_streams").doc("stream_1").update({
      likeCount: 10,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("StreamMessages: real-time chat & gift events require parent stream and filtered query", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("live_streams").doc("stream_chat_1").set(validStreamPayload(ALICE_UID, now))
  );

  // Valid gift message
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

  // Sender spoofing rejected
  await assertFails(
    bobDb.collection("live_streams").doc("stream_chat_1").collection("messages").doc("msg_spoof").set({
      streamId: "stream_chat_1",
      streamVisibility: "PUBLIC",
      hostId: ALICE_UID,
      senderId: ALICE_UID,
      senderName: "Alice",
      senderLevel: 12,
      messageType: "CHAT",
      body: "Spoofed!",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Filtered query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("live_streams").doc("stream_chat_1").collection("messages")
      .where("streamVisibility", "==", "PUBLIC").get()
  );
  await assertFails(
    bobDb.collection("live_streams").doc("stream_chat_1").collection("messages").get()
  );
});
