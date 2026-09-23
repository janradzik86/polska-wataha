import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { mayParentSee } from "./access.ts";

describe("dostęp do pozycji dziecka", () => {
  it("rodzic z aktywnym linkiem widzi, admin i odwołany link nie", () => {
    assert.equal(
      mayParentSee({
        viewerId: "rodzic",
        childId: "dziecko",
        viewerIsAdmin: false,
        linkStatus: "active",
        parentUserId: "rodzic",
        consent: "while_app_open",
      }).ok,
      true,
    );
    assert.equal(
      mayParentSee({
        viewerId: "admin",
        childId: "dziecko",
        viewerIsAdmin: true,
        linkStatus: "active",
        parentUserId: "rodzic",
        consent: "while_app_open",
      }).ok,
      false,
    );
    assert.equal(
      mayParentSee({
        viewerId: "rodzic",
        childId: "dziecko",
        viewerIsAdmin: false,
        linkStatus: "revoked",
        parentUserId: "rodzic",
        consent: "while_app_open",
      }).ok,
      false,
    );
    assert.equal(
      mayParentSee({
        viewerId: "rodzic",
        childId: "dziecko",
        viewerIsAdmin: false,
        linkStatus: "active",
        parentUserId: "rodzic",
        consent: "off",
      }).ok,
      false,
    );
  });
});
