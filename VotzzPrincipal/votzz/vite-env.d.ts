/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_PLAN_ESSENTIAL: string
  readonly VITE_PLAN_BUSINESS: string
  readonly VITE_PLAN_CUSTOM: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}