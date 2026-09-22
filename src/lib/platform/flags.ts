export const FLAG_DEFAULTS = {
  enableOfflineMaps: true,
  enableOfflineNavigation: true,
  enableChildLocation: true,
  enableBackgroundLocation: false,
  enableFamilyBridge: true,
  enableSosAudio: true,
  enableKnowledgeUpdates: true,
  enableOfflineVoice: false,
  enableYoungWolfGame: false,
} as const;

export type FlagName = keyof typeof FLAG_DEFAULTS;
