/** Odbiornik na telefonie opiekuna. Bez zdalnej ścieżki nie nagrywa i nie udaje live. */
export class ParentSosReceiver {
  recording = false;

  onRemoteAudioTrack(present: boolean): string {
    if (!present) {
      this.recording = false;
      return "Łączenie z mikrofonem dziecka…";
    }
    this.recording = true;
    return "Odbieram dźwięk. Nagranie zostaje na tym urządzeniu.";
  }

  stop() {
    this.recording = false;
  }

  fileName(childName: string, stamp: string): string {
    const safe = childName.replace(/[^\p{L}\p{N}_-]+/gu, "") || "dziecko";
    return `SOS_${safe}_${stamp}.m4a`;
  }
}

export type RecordingMeta = {
  sessionId: string;
  childId: string;
  startedAt: string;
  endedAt: string | null;
  lastKnownLat: number | null;
  lastKnownLon: number | null;
  accuracy: number | null;
  sha256: string | null;
  fileName: string;
};

export function emptyRecording(input: Omit<RecordingMeta, "sha256">): RecordingMeta {
  return { ...input, sha256: null };
}
