import { create } from "zustand";
import { persist } from "zustand/middleware";

export type QueuedAction = {
  id: string;
  action: string;
  payload: string;
  createdAt: string;
};

type SiatkaState = {
  crisis: boolean;
  setCrisis: (v: boolean) => void;
  online: boolean;
  setOnline: (v: boolean) => void;
  simulateOffline: boolean;
  setSimulateOffline: (v: boolean) => void;
  queue: QueuedAction[];
  enqueue: (item: Omit<QueuedAction, "id" | "createdAt">) => void;
  clearQueue: () => void;
};

export const useSiatka = create<SiatkaState>()(
  persist(
    (set) => ({
      crisis: false,
      setCrisis: (crisis) => set({ crisis }),
      online: true,
      setOnline: (online) => set({ online }),
      simulateOffline: false,
      setSimulateOffline: (simulateOffline) => set({ simulateOffline }),
      queue: [],
      enqueue: (item) =>
        set((s) => ({
          queue: [
            {
              ...item,
              id: `local_${Date.now().toString(36)}`,
              createdAt: new Date().toISOString(),
            },
            ...s.queue,
          ].slice(0, 50),
        })),
      clearQueue: () => set({ queue: [] }),
    }),
    { name: "siatka-local", partialize: (s) => ({ queue: s.queue, simulateOffline: s.simulateOffline }) },
  ),
);

export function isEffectivelyOnline() {
  const s = useSiatka.getState();
  return s.online && !s.simulateOffline;
}
