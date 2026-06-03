import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthStateService } from '../../../core/services/auth-state.service';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

@Component({
  selector: 'nx-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-assistant.component.html',
  styleUrl: './ai-assistant.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiAssistantComponent {

  readonly isOpen = signal(false);
  readonly isMinimized = signal(false);
  readonly isLoading = signal(false);
  readonly messages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      content: 'Bonjour ! Je suis votre assistant NexusERP. Comment puis-je vous aider ?',
      timestamp: new Date()
    }
  ]);

  userInput = '';

  constructor(
    private http: HttpClient,
    private authState: AuthStateService
  ) {}

  togglePanel(): void {
    this.isOpen.update(v => !v);
    this.isMinimized.set(false);
  }

  toggleMinimize(): void {
    this.isMinimized.update(v => !v);
  }

  async sendMessage(): Promise<void> {
    const input = this.userInput.trim();
    if (!input || this.isLoading()) return;

    // Ajouter message utilisateur
    this.messages.update(msgs => [
      ...msgs,
      { role: 'user', content: input, timestamp: new Date() }
    ]);
    this.userInput = '';
    this.isLoading.set(true);

    try {
      const response = await this.http.post<any>('/api/v1/ai/chat', {
        messages: this.messages().map(m => ({ role: m.role, content: m.content })),
        module: 'general'
      }).toPromise();

      this.messages.update(msgs => [
        ...msgs,
        {
          role: 'assistant',
          content: response.message.content,
          timestamp: new Date()
        }
      ]);
    } catch (error) {
      this.messages.update(msgs => [
        ...msgs,
        {
          role: 'assistant',
          content: 'Désolé, une erreur s\'est produite. Veuillez réessayer.',
          timestamp: new Date()
        }
      ]);
    } finally {
      this.isLoading.set(false);
    }
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}
