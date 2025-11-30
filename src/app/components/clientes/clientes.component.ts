import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ClienteService, Cliente } from '../../services/cliente.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-clientes',
  templateUrl: './clientes.component.html',
  styleUrls: ['./clientes.component.css']
})
export class ClientesComponent implements OnInit {
  
  clientes: Cliente[] = [];
  clienteForm: FormGroup;
  pontosForm: FormGroup;
  showModal = false;
  showDetalhesModal = false;
  editingCliente: Cliente | null = null;
  clienteDetalhes: any = null;
  searchTerm = '';
  selectedCategory = '';
  
  categoryFilter = '';
  statusFilter = '';
  
  constructor(
    private fb: FormBuilder,
    private clienteService: ClienteService,
    public authService: AuthService
  ) {
    this.clienteForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      telefone: ['', [Validators.required]],
      cpf: ['', [Validators.required]],
      endereco: this.fb.group({
        rua: ['', [Validators.required]],
        numero: [''],
        complemento: [''],
        bairro: [''],
        cidade: [''],
        cep: [''],
        estado: ['']
      }),
      dataNascimento: ['']
    });

    this.pontosForm = this.fb.group({
      pontos: ['', [Validators.required, Validators.min(1)]],
      motivo: ['ajuste_manual', [Validators.required]],
      observacoes: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.loadClientes();
  }

  loadClientes(): void {
    this.clienteService.listarClientes({ ativo: true }, 0, 20).subscribe(response => {
      this.clientes = response.content;
    });
  }

  openModal(cliente?: Cliente): void {
    this.showModal = true;
    this.editingCliente = cliente || null;
    
    if (cliente) {
      this.clienteForm.patchValue(cliente);
    } else {
      this.clienteForm.reset();
    }
  }

  closeModal(): void {
    this.showModal = false;
    this.editingCliente = null;
    this.clienteForm.reset();
  }

  onSubmit(): void {
    if (this.clienteForm.valid) {
      const clienteData = this.clienteForm.value;
      
      if (this.editingCliente) {
        this.clienteService.atualizarCliente(this.editingCliente.id!, clienteData).subscribe(() => {
          this.loadClientes();
          this.closeModal();
        });
      } else {
        this.clienteService.criarCliente(clienteData).subscribe(() => {
          this.loadClientes();
          this.closeModal();
        });
      }
    }
  }

  deleteCliente(id: number): void {
    if (confirm('Tem certeza que deseja excluir este cliente?')) {
      this.clienteService.deletarCliente(id).subscribe(() => {
        this.loadClientes();
      });
    }
  }

  get filteredClientes(): Cliente[] {
    return this.clientes.filter(cliente => {
      const matchesSearch = !this.searchTerm || 
        cliente.nome.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (cliente.email && cliente.email.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
        (cliente.cpf && cliente.cpf.includes(this.searchTerm));
        
      const matchesCategory = true; // !this.categoryFilter || cliente.categoria === this.categoryFilter;
      
      return matchesSearch && matchesCategory;
    });
  }

  getCategoriaClass(categoria: string): string {
    switch (categoria) {
      case 'Diamante':
        return 'bg-primary';
      case 'Ouro':
        return 'bg-warning';
      case 'Prata':
        return 'bg-secondary';
      default:
        return 'bg-dark';
    }
  }

  visualizarPerfilCliente(cliente: Cliente): void {
    console.log('Visualizando perfil do cliente:', cliente);
    
    this.clienteDetalhes = {
      cliente: cliente,
      vendas: [], // TODO: Implementar busca de vendas do cliente
      pontosFidelidade: 0, // TODO: Implementar sistema de pontos
      ultimaCompra: undefined, // TODO: Buscar última compra
      totalGasto: 0 // TODO: Calcular total gasto
    };
    
    const modalElement = document.getElementById('detalhesModal');
    if (modalElement) {
      const modal = new (window as any).bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  verDetalhesCompletos(cliente: Cliente): void {
    console.log('Carregando detalhes completos do cliente:', cliente);
    
    if (cliente.id) {
      this.clienteService.buscarPorId(cliente.id).subscribe({
        next: (clienteCompleto) => {
          this.clienteDetalhes = {
            cliente: clienteCompleto,
            vendas: [], // TODO: Implementar endpoint de vendas por cliente
            pontosFidelidade: 0, // TODO: Implementar sistema de pontos
            ultimaCompra: undefined, // TODO: Buscar última compra
            totalGasto: 0 // TODO: Calcular total gasto
          };
          
          const modalElement = document.getElementById('detalhesModal');
          if (modalElement) {
            const modal = new (window as any).bootstrap.Modal(modalElement);
            modal.show();
          }
        },
        error: (error) => {
          console.error('❌ Erro ao carregar detalhes do cliente:', error);
          this.visualizarPerfilCliente(cliente);
        }
      });
    } else {
      this.visualizarPerfilCliente(cliente);
    }
  }

  fecharDetalhesModal(): void {
    this.showDetalhesModal = false;
    this.clienteDetalhes = null;
  }

  ajustarPontos(): void {
    if (this.pontosForm.valid && this.clienteDetalhes) {
      const formData = this.pontosForm.value;
      const clienteId = this.clienteDetalhes.cliente.id;
      
      console.log('Ajustando pontos do cliente:', clienteId, formData.pontos);
      // this.clienteService.adicionarPontos(clienteId, parseInt(formData.pontos), formData.observacoes || 'Ajuste manual')
      
      this.fecharDetalhesModal();
      this.loadClientes();
    }
  }

  formatDate(date: Date | string | null): string {
    if (!date) return 'Não informado';
    
    const d = new Date(date);
    if (isNaN(d.getTime())) return 'Data inválida';
    
    return d.toLocaleDateString('pt-BR');
  }
}
