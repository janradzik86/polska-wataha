import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { ParentSosReceiver } from "../family/sos-receiver.ts";
import { BrouterRoutingProvider, PackGraphRoutingProvider, requestPackDownload } from "./offline.ts";

describe("mapy i SOS rodzica", () => {
  it("nie pobiera w kryzysie ani offline i nie udaje routera", () => {
    assert.equal(requestPackDownload({ crisis: true, online: true }).ok, false);
    assert.equal(requestPackDownload({ crisis: false, online: false }).ok, false);
    assert.match(requestPackDownload({ crisis: false, online: true }).reason, /tile.openstreetmap.org/);
    assert.equal(new BrouterRoutingProvider().available, false);
    assert.equal(new BrouterRoutingProvider().plan(), null);
    assert.equal(new PackGraphRoutingProvider().plan(), null);
  });

  it("nie nagrywa bez ścieżki audio", () => {
    const receiver = new ParentSosReceiver();
    assert.equal(receiver.recording, false);
    assert.match(receiver.onRemoteAudioTrack(false), /Łączenie/);
    assert.equal(receiver.recording, false);
    receiver.onRemoteAudioTrack(true);
    assert.equal(receiver.recording, true);
    receiver.stop();
    assert.equal(receiver.recording, false);
    assert.match(receiver.fileName("Lena", "2026-09-22_21-43"), /^SOS_Lena_2026-09-22_21-43\.m4a$/);
  });
});
