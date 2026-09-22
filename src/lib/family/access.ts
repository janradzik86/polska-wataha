export type ShareMode = "off" | "while_app_open" | "background";
export type LinkStatus = "pending_code" | "pending_consent" | "active" | "revoked";

/** Ta sama reguła co w Młodej Watasze. Aplikacje nie dzielą kodu. */
export function mayParentSee(input: {
  viewerId: string;
  childId: string;
  viewerIsAdmin: boolean;
  linkStatus: LinkStatus | null;
  parentUserId: string | null;
  consent: ShareMode;
}): { ok: boolean; reason: string } {
  if (input.viewerId === input.childId) return { ok: true, reason: "swoje" };
  if (input.viewerIsAdmin && input.parentUserId !== input.viewerId) return { ok: false, reason: "admin-nie-widzi" };
  if (input.linkStatus !== "active" || input.parentUserId !== input.viewerId) return { ok: false, reason: "brak-parent-link" };
  if (input.consent === "off") return { ok: false, reason: "brak-zgody" };
  return { ok: true, reason: "parent-link" };
}
