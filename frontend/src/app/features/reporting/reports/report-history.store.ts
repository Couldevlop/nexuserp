import { Injectable } from '@angular/core';
import { ReportDto } from './reporting-format';

/**
 * Historique local des demandes de rapports.
 *
 * Le backend nexus-reporting ne fournit pas d'endpoint de liste : seul le
 * statut individuel (`GET /api/v1/reports/{id}/status`) est exposé. On conserve
 * donc localement (localStorage) la liste des rapports demandés depuis ce
 * navigateur afin de pouvoir afficher une liste exploitable et rafraîchir
 * leur statut. Aucune donnée n'est inventée : seules les demandes réellement
 * acceptées par le serveur (202) sont stockées.
 */
@Injectable({ providedIn: 'root' })
export class ReportHistoryStore {
  private static readonly KEY = 'nexuserp.reporting.jobs';
  private static readonly MAX = 100;

  /** Lit l'historique trié par date de demande décroissante. */
  list(): ReportDto[] {
    try {
      const raw = localStorage.getItem(ReportHistoryStore.KEY);
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return [];
      }
      return (parsed as ReportDto[])
        .filter((r) => r && typeof r.id === 'string')
        .sort((a, b) => (b.requestedAt ?? '').localeCompare(a.requestedAt ?? ''));
    } catch {
      return [];
    }
  }

  /** Ajoute ou remplace un rapport dans l'historique. */
  upsert(report: ReportDto): void {
    const all = this.list().filter((r) => r.id !== report.id);
    all.unshift(report);
    this.persist(all.slice(0, ReportHistoryStore.MAX));
  }

  /** Supprime un rapport de l'historique local. */
  remove(id: string): void {
    this.persist(this.list().filter((r) => r.id !== id));
  }

  private persist(reports: ReportDto[]): void {
    try {
      localStorage.setItem(ReportHistoryStore.KEY, JSON.stringify(reports));
    } catch {
      // Quota dépassé / mode privé : on ignore silencieusement.
    }
  }
}
