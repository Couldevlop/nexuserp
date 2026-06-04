import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Configuration Capacitor — wrapper mobile (Android) de NexusERP.
 *
 * `webDir` DOIT pointer vers la sortie réelle du build Angular 18 (application builder).
 * Voir angular.json → architect.build.options.outputPath = "dist/nexuserp"
 * Le builder `@angular-devkit/build-angular:application` écrit les assets navigateur
 * dans le sous-dossier `browser`, d'où `dist/nexuserp/browser`.
 *
 * Génération de la plateforme Android (sur une machine outillée) :
 *   npm install
 *   npm run build:mobile      # ng build + cap sync
 *   npx cap add android       # première fois uniquement
 *   npm run cap:open:android  # ouvre Android Studio
 */
const config: CapacitorConfig = {
  appId: 'io.nexuserp.app',
  appName: 'NexusERP',
  webDir: 'dist/nexuserp/browser',
  android: {
    // Sert le contenu via https:// (origine sécurisée) plutôt que http://localhost
    // -> requis pour Service Worker / PWA, crypto.subtle, et cookies SameSite.
    allowMixedContent: false,
  },
  server: {
    androidScheme: 'https',
    // --- Live-reload contre le serveur de dev Angular (décommenter au besoin) ---
    // Remplacer l'IP par celle de la machine de dev sur le réseau local,
    // lancer `npm start -- --host 0.0.0.0`, puis `npm run cap:sync` et relancer l'app.
    // url: 'http://192.168.1.10:4200',
    // cleartext: true,
  },
  plugins: {
    // Délai d'affichage du splash (ms) — ajustable.
    SplashScreen: {
      launchShowDuration: 800,
      backgroundColor: '#0F172A',
    },
  },
};

export default config;
