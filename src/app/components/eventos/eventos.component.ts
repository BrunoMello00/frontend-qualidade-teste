import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EventosService, Evento } from '../../services/eventos.service';

@Component({
  selector: 'app-eventos',
  templateUrl: './eventos.component.html',
  styleUrls: ['./eventos.component.css']
})
export class EventosComponent implements OnInit {
  
  eventos: Evento[] = [];
  eventoForm: FormGroup;
  showModal = false;
  editingEvento: Evento | null = null;
  searchTerm = '';
  
  isLoading = false;
  isSaving = false;
  
  constructor(
    private fb: FormBuilder,
    private eventosService: EventosService
  ) {
    this.eventoForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      descricao: ['', [Validators.required]],
      dataInicio: ['', [Validators.required]],
      dataFim: ['', [Validators.required]],
      desconto: ['', [Validators.min(0), Validators.max(100)]]
    });
  }

  ngOnInit(): void {
    this.loadEventos();
  }

  loadEventos(): void {
    this.isLoading = true;
    this.eventosService.listarEventos().subscribe({
      next: (response: any) => {
        console.log('📥 Resposta do backend:', response);
        
        if (Array.isArray(response)) {
          this.eventos = response;
        } else if (Array.isArray(response?.content)) {
          this.eventos = response.content;
        } else {
          this.eventos = [];
        }
        
        console.log('🔍 Primeiro evento (debug):', this.eventos[0]);
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erro ao carregar eventos:', error);
        this.eventos = [];
        this.isLoading = false;
      }
    });
  }

  openModal(evento?: Evento): void {
    this.showModal = true;
    this.editingEvento = evento || null;
    
    if (evento) {
      this.eventoForm.patchValue({
        nome: evento.nome || '',
        descricao: evento.descricao || '',
        dataInicio: evento.dataInicio ? this.formatDateForInput(evento.dataInicio) : '',
        dataFim: evento.dataFim ? this.formatDateForInput(evento.dataFim) : '',
        desconto: evento.descontoPercentual || ''
      });
    } else {
      this.eventoForm.reset();
      this.eventoForm.patchValue({ desconto: '' });
    }
  }

  closeModal(): void {
    this.showModal = false;
    this.editingEvento = null;
    this.eventoForm.reset();
  }

  onSubmit(): void {
    if (this.eventoForm.valid) {
      this.isSaving = true;
      const formValue = this.eventoForm.value;
      console.log('📝 Form values:', formValue);
      
      const eventoData = {
        nome: formValue.nome,
        descricao: formValue.descricao,
        dataInicio: formValue.dataInicio ? formValue.dataInicio.split('T')[0] : null, // Remove hora, mantém apenas YYYY-MM-DD
        dataFim: formValue.dataFim ? formValue.dataFim.split('T')[0] : null,         // Remove hora, mantém apenas YYYY-MM-DD
        descontoPercentual: formValue.desconto ? Number(formValue.desconto) : undefined,
        status: 'PLANEJADO'
      };
      
      console.log('📤 Dados enviados:', eventoData);
      
      if (this.editingEvento) {
        this.eventosService.atualizarEvento(this.editingEvento.id!, eventoData).subscribe({
          next: () => {
            this.loadEventos();
            this.closeModal();
            this.isSaving = false;
          },
          error: (error) => {
            console.error('Erro ao atualizar evento:', error);
            alert('Erro ao atualizar evento. Tente novamente.');
            this.isSaving = false;
          }
        });
      } else {
        this.eventosService.criarEvento(eventoData).subscribe({
          next: () => {
            this.loadEventos();
            this.closeModal();
            this.isSaving = false;
          },
          error: (error) => {
            console.error('Erro ao criar evento:', error);
            console.log('Detalhes do erro:', error.error);
            if (error.error?.validationErrors) {
              console.log('Erros de validação:', error.error.validationErrors);
              const erros = Object.values(error.error.validationErrors).join('\n');
              alert(`Erro de validação:\n${erros}`);
            } else {
              alert('Erro ao criar evento. Tente novamente.');
            }
            this.isSaving = false;
          }
        });
      }
    }
  }

  deleteEvento(id: number): void {
    if (confirm('Tem certeza que deseja excluir este evento?')) {
      this.eventosService.deletarEvento(id).subscribe({
        next: () => {
          this.loadEventos();
        },
        error: (error: any) => {
          console.error('Erro ao excluir evento:', error);
          alert('Erro ao excluir evento. Tente novamente.');
        }
      });
    }
  }

  toggleEvento(evento: Evento): void {
    
    if (evento.id) {
      if (evento.ativo) {
        this.eventosService.deletarEvento(evento.id).subscribe({
          next: () => {
            this.loadEventos(); // Recarrega a lista
          },
          error: (error: any) => {
            console.error('❌ Erro ao desativar evento:', error);
            alert('Erro ao desativar evento. Tente novamente.');
          }
        });
      } else {
        this.eventosService.reativarEvento(evento.id).subscribe({
          next: () => {
            this.loadEventos(); // Recarrega a lista
          },
          error: (error: any) => {
            console.error('❌ Erro ao reativar evento:', error);
            alert('Erro ao reativar evento. Tente novamente.');
          }
        });
      }
    } else {
      console.warn('⚠️ Evento sem ID válido:', evento);
    }
  }

  get filteredEventos(): Evento[] {
    return this.eventos.filter(evento => {
      const matchesSearch = !this.searchTerm || 
        (evento.nome && evento.nome.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
        (evento.descricao && evento.descricao.toLowerCase().includes(this.searchTerm.toLowerCase()));
        
      return matchesSearch;
    });
  }

  hasActiveFilters(): boolean {
    return !!this.searchTerm;
  }

  isFilterActive(field: string): boolean {
    switch (field) {
      case 'search': return !!this.searchTerm;
      default: return false;
    }
  }

  limparFiltros(): void {
    this.searchTerm = '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.eventoForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  formatDateTime(dateValue: string): string {
    if (!dateValue) return '';
    
    const date = new Date(dateValue);
    if (isNaN(date.getTime())) return '';
    
    return date.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit', 
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  private formatDateForInput(date: Date | string): string {
    const d = new Date(date);
    return d.toISOString().slice(0, 16);
  }

  getStatusClass(ativo: boolean): string {
    return ativo ? 'bg-success' : 'bg-secondary';
  }

  isEventoAtivo(evento: Evento): boolean {
    const now = new Date();
    const inicio = evento.dataInicio ? new Date(evento.dataInicio) : null;
    const fim = evento.dataFim ? new Date(evento.dataFim) : null;
    return !!(evento.ativo && inicio && fim && now >= inicio && now <= fim);
  }
}
