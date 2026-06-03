import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { OAuthService } from 'angular-oauth2-oidc';

@Component({
  selector: 'nx-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class AppComponent implements OnInit {

  constructor(
    private translate: TranslateService,
    private oauthService: OAuthService
  ) {}

  ngOnInit(): void {
    this.initI18n();
    this.oauthService.setupAutomaticSilentRefresh();
  }

  private initI18n(): void {
    this.translate.addLangs(['fr-FR', 'fr-CI', 'en-US', 'en-GB']);
    this.translate.setDefaultLang('fr-FR');
    const browserLang = this.translate.getBrowserLang();
    const lang = browserLang?.startsWith('fr') ? 'fr-FR' : 'fr-FR';
    this.translate.use(lang);
  }
}
