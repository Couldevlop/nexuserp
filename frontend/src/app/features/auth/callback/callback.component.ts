import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';

@Component({
  selector: 'nx-callback',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './callback.component.html',
  styleUrl: './callback.component.scss'
})
export class CallbackComponent implements OnInit {
  private readonly oauthService = inject(OAuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    // angular-oauth2-oidc processes the code automatically in app.component.ts
    // This component just shows a loading state while the token is exchanged
    const checkToken = setInterval(() => {
      if (this.oauthService.hasValidAccessToken()) {
        clearInterval(checkToken);
        this.router.navigate(['/dashboard']);
      }
    }, 200);

    // Failsafe: redirect after 10s
    setTimeout(() => {
      clearInterval(checkToken);
      this.router.navigate(['/dashboard']);
    }, 10_000);
  }
}
