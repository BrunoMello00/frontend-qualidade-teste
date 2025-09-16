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
  
  constructor(
    private fb: FormBuilder,
    private eventosService: EventosService
  ) {
    this.eventoForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      descricao: ['', [Validators.required]],
      dataInicio: ['', [Validators.required]],
      dataFim: ['', [Validators.required]],
      desconto: ['', [Validators.min(0), Validators.max(100)]],
      ativo: [true]
    });
  }

  ngOnInit(): void {
    this.loadEventos();
  }

  loadEventos(): void {
    this.eventosService.listarEventos().subscribe((res: any) => {
      // o mock pode retornar um objeto paginado { content, totalElements }
      if (res && Array.isArray(res.content)) {
        this.eventos = res.content;
      } else if (Array.isArray(res)) {
        this.eventos = res as Evento[];
      } else {
        this.eventos = [];
      }
    });
  }

  openModal(evento?: Evento): void {
    this.showModal = true;
    this.editingEvento = evento || null;
    
    if (evento) {
      this.eventoForm.patchValue({
        ...evento,
        dataInicio: evento.dataInicio ? this.formatDateForInput(evento.dataInicio) : '',
        dataFim: evento.dataFim ? this.formatDateForInput(evento.dataFim) : ''
      });
    } else {
      this.eventoForm.reset();
      this.eventoForm.patchValue({ ativo: true, desconto: '' });
    }
  }

  closeModal(): void {
    this.showModal = false;
    this.editingEvento = null;
    this.eventoForm.reset();
  }

  onSubmit(): void {
    if (this.eventoForm.valid) {
      const eventoData = {
        ...this.eventoForm.value,
        dataInicio: new Date(this.eventoForm.value.dataInicio),
        dataFim: new Date(this.eventoForm.value.dataFim)
      };
      
      if (this.editingEvento) {
        // Atualizar evento
        this.eventosService.atualizarEvento(this.editingEvento.id!, eventoData).subscribe({
          next: () => {
            this.loadEventos();
            this.closeModal();
          },
          error: (error) => {
            console.error('Erro ao atualizar evento:', error);
            alert('Erro ao atualizar evento. Tente novamente.');
          }
        });
      } else {
        // Criar novo evento
        this.eventosService.criarEvento(eventoData).subscribe({
          next: () => {
            this.loadEventos();
            this.closeModal();
          },
          error: (error) => {
            console.error('Erro ao criar evento:', error);
            alert('Erro ao criar evento. Tente novamente.');
          }
        });
      }
    }
  }

  deleteEvento(id: number): void {
    if (confirm('Tem certeza que deseja excluir este evento?')) {
      this.eventosService.excluirEvento(id).subscribe({
        next: () => {
          this.loadEventos();
        },
        error: (error) => {
          console.error('Erro ao excluir evento:', error);
          alert('Erro ao excluir evento. Tente novamente.');
        }
      });
    }
  }

  toggleEvento(evento: Evento): void {
    if (evento.id) {
      const novoAtivo = !evento.ativo;
      const novoStatusStr = novoAtivo ? 'CONFIRMADO' : 'CANCELADO';
      this.eventosService.alterarStatusEvento(evento.id, novoStatusStr).subscribe({
        next: (eventoAtualizado: any) => {
          evento.ativo = eventoAtualizado.ativo !== undefined ? eventoAtualizado.ativo : (eventoAtualizado.status !== 'CANCELADO');
          evento.status = eventoAtualizado.status || novoStatusStr;
        },
        error: (error) => {
          console.error('Erro ao alterar status do evento:', error);
          alert('Erro ao alterar status do evento. Tente novamente.');
        }
      });
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
