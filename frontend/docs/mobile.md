# NexusERP Mobile (Android) — Capacitor Wrapper

Ce document décrit comment générer, construire et exécuter l'application mobile
Android de NexusERP à partir du frontend Angular 18 existant, via **Capacitor 6**.

> Le code et la configuration sont déjà en place dans le dépôt. Les commandes
> ci-dessous doivent être exécutées sur une **machine correctement outillée**
> (registre npm accessible, Android SDK + Android Studio installés).

---

## 1. Prérequis

| Outil | Version recommandée | Notes |
| --- | --- | --- |
| Node.js | 20 LTS+ | identique au build web |
| JDK | 17 (Temurin) | requis par Android Gradle Plugin |
| Android Studio | Ladybug+ | inclut SDK Manager + AVD |
| Android SDK | API 34 (compile/target) | via SDK Manager |
| Variables env | `ANDROID_HOME` / `JAVA_HOME` | pointant SDK et JDK |

Un appareil physique (mode développeur + débogage USB) **ou** un émulateur AVD
est nécessaire pour exécuter l'app. Le **scan de code-barres requiert une caméra**
(les émulateurs récents émulent la caméra ; un appareil physique est conseillé).

---

## 2. Installation des dépendances

Depuis `frontend/` :

```bash
npm install
```

Cela installe Capacitor et les plugins déjà déclarés dans `package.json` :

- `@capacitor/core`, `@capacitor/cli`, `@capacitor/android`
- `@capacitor/app`, `@capacitor/network`, `@capacitor/camera`
- `@capacitor-mlkit/barcode-scanning` (scan caméra ML Kit)

---

## 3. Génération de la plateforme Android (première fois)

```bash
# 1) Construire le web (sortie dans dist/nexuserp/browser — voir capacitor.config.ts)
npm run build:mobile        # = ng build (prod) + cap sync

# 2) Ajouter la plateforme native Android (UNE SEULE FOIS)
npx cap add android

# 3) Synchroniser à nouveau pour copier les assets web + plugins
npm run cap:sync
```

Le dossier `android/` est alors créé (projet Gradle natif). Il peut être
versionné ou régénéré ; `cap sync` recopie le build web à chaque fois.

`capacitor.config.ts` :

- `appId` : `io.nexuserp.app`
- `appName` : `NexusERP`
- `webDir` : `dist/nexuserp/browser` (sortie réelle de l'Angular *application builder*)
- `server.androidScheme` : `https` (origine sécurisée → Service Worker / crypto OK)

---

## 4. Construire / lancer l'application

### Via Android Studio (recommandé)

```bash
npm run cap:open:android    # ouvre le projet android/ dans Android Studio
```

Puis dans Android Studio : sélectionner un appareil/émulateur et cliquer **Run ▶**.
Le build du **`.apk` / `.aab`** se fait via Gradle (Build > Build Bundle(s) / APK(s)).

### Via la ligne de commande

```bash
npm run cap:android         # = cap run android (build + déploiement sur device)
```

### Cycle de mise à jour du code web

À chaque modification du frontend Angular :

```bash
npm run build:mobile        # rebuild web + cap sync
# puis relancer depuis Android Studio ou: npm run cap:android
```

---

## 5. Live-reload contre le serveur de dev Angular

Pour itérer sans rebuild natif, on pointe l'app vers le serveur `ng serve` du PC.

1. Démarrer le dev server exposé sur le réseau local :

   ```bash
   npm start -- --host 0.0.0.0     # écoute sur 0.0.0.0:4200
   ```

2. Dans `capacitor.config.ts`, **décommenter** et adapter le bloc `server` :

   ```ts
   server: {
     androidScheme: 'https',
     url: 'http://192.168.1.10:4200', // IP LAN de la machine de dev
     cleartext: true,
   },
   ```

3. Synchroniser et relancer :

   ```bash
   npm run cap:sync
   npm run cap:android
   ```

L'appareil et le PC doivent être sur le **même réseau Wi-Fi**. Le rechargement
du navigateur Angular se répercute en direct dans l'app.

> **Important** : recommenter / vider `server.url` avant un build de release,
> sinon l'app pointerait vers un serveur de dev inexistant en production.

---

## 6. Permissions Android

Le scan caméra nécessite la permission `CAMERA`. Le plugin
`@capacitor-mlkit/barcode-scanning` l'ajoute au manifeste lors du `cap add` ;
vérifier dans `android/app/src/main/AndroidManifest.xml` la présence de :

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

La demande de permission à l'exécution est gérée par `BarcodeScannerService`
(`requestPermissions()`), avec dégradation gracieuse si refusée.

---

## 7. Architecture web-safe des bridges natifs

Les wrappers natifs (`src/app/core/native/`) **ne cassent jamais le build web** :

- aucun `import` statique de paquet `@capacitor/*` dans le code applicatif ;
- détection du runtime via `window.Capacitor` (`isNativeRuntime()`) ;
- chargement des plugins en **`import()` dynamique** (`loadPlugin()`), encapsulé
  dans un `try/catch` → renvoie `null` proprement quand le paquet est absent (web/test).

Services exposés (`core/native`) :

| Service | Rôle | Comportement web |
| --- | --- | --- |
| `PlatformService` | `isNative`, `platform` (signaux) | `false` / `'web'` |
| `BarcodeScannerService` | `scan(): Promise<string \| null>` | renvoie `null` |
| `NativeNetworkService` | pont `@capacitor/network` → signal | `init()` no-op |

Composant partagé : `nx-barcode-button` (`shared/components/barcode-button/`),
masqué sur le web par défaut (`hideOnWeb`), câblé dans la recherche d'articles
(`features/inventory/products/product-list`) comme démonstration.

---

## 8. Mobile Money — prêt à brancher

Les flux Mobile Money (Orange/MTN/Wave, BCEAO/UEMOA) s'appuieront sur :

- `NativeNetworkService` pour fiabiliser l'état réseau avant initiation de paiement ;
- `@capacitor/app` (deep links) pour le retour depuis l'app opérateur / la page
  de confirmation (callback `appUrlOpen`) ;
- le sous-système offline existant (`core/offline`) pour mettre en file les
  intentions de paiement hors-ligne et les rejouer à la reconnexion.

Aucun secret opérateur ne doit être embarqué dans l'app : l'initiation passe par
le backend (`nexus-finance` / gateway).
