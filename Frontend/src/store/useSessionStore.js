import { create } from "zustand";

export const useSessionStore = create((set) => ({
  tasaUsd: null,
  setTasaUsd: (tasa) => set({ tasaUsd: tasa }),
  sidebarColapsado: false,
  toggleSidebar: () => set((s) => ({ sidebarColapsado: !s.sidebarColapsado })),
}));
