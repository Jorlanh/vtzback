// src/types.ts - ARQUIVO DE DEFINIÇÕES COMPLETO

// --- Tipos Globais de Usuário ---
export type UserRole = 'ADMIN' | 'SINDICO' | 'ADM_CONDO' | 'MORADOR' | 'AFILIADO' | 'MANAGER';

// --- Autenticação ---
export interface LoginCredentials { // Adicionado conforme solicitado para AuthContext
  email: string;
  password: string;
}

export interface LoginRequest {
  login: string; // Pode ser email ou CPF
  password: string;
  selectedProfileId?: string; // Para fluxo multi-perfil
}

export interface User {
  id: string;
  nome: string;      
  name?: string;     // Tornamos opcional para evitar erro se vier apenas 'nome'
  email: string;
  cpf?: string;      
  unidade?: string;  
  unit?: string;     // Compatibilidade
  bloco?: string;    
  block?: string;    // Compatibilidade
  whatsapp?: string;
  phone?: string;    // Compatibilidade
  role: UserRole;
  tenant?: Tenant;   // Referência ao objeto Tenant completo se disponível
  tenantId?: string | null;
  fraction?: number;
  is2faEnabled?: boolean;
  avatarUrl?: string;
  lastSeen?: string;
}

export interface Tenant {
  id: string;
  nome: string;
  cnpj?: string;
  unidadesTotal?: number;
  plano?: any;
}

export interface ProfileOption {
  userId: string;
  userName: string;
  role: string;
  contextName: string;
}

export interface LoginResponse {
  token: string;
  type?: string;     
  id: string;        
  nome: string;
  email: string;
  role: UserRole;
  tenantId?: string | null;
  bloco?: string;
  unidade?: string;
  cpf?: string;

  // Multi-Context Fields
  multipleProfiles?: boolean;
  profiles?: ProfileOption[];
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (data: LoginResponse) => void;
  logout: () => void;
  updateUser: (data: Partial<User>) => void;
  selectContext: (userId: string) => Promise<void>;
}

// --- Dashboards e Métricas ---
export interface AdminDashboardStats {
  totalTenants: number;
  activeTenants: number;
  totalUsers: number;
  onlineUsers: number;
  mrr: number; 
  latencyMs?: number; 
}

export interface AffiliateDashboardDTO {
  saldoDisponivel: number;
  saldoFuturo: number;
  linkIndicacao: string;
  vendasTotais?: number;
  cliquesLink?: number;
}

// --- Assembleias e Votação ---
export enum VoteType {
  YES_NO_ABSTAIN = 'YES_NO_ABSTAIN',
  MULTIPLE_CHOICE = 'MULTIPLE_CHOICE',
  CUSTOM = 'CUSTOM'
}

export enum VotePrivacy {
  OPEN = 'OPEN',
  SECRET = 'SECRET'
}

export enum AssemblyStatus {
  DRAFT = 'DRAFT',
  OPEN = 'OPEN',
  CLOSED = 'CLOSED',
  AGENDADA = 'AGENDADA',
  SCHEDULED = 'SCHEDULED'
}

export interface Assembly {
  id: string;
  
  // Campos de Texto (Aceita PT ou EN)
  title?: string;        // Opcional: Backend pode mandar 'titulo'
  titulo?: string;       // Opcional: Backend pode mandar 'title'
  description: string;
  
  // Status Híbrido (Enum ou String crua para flexibilidade)
  status: AssemblyStatus | string; 
  
  // Datas (Aceita PT ou EN)
  startDate?: string;    // Opcional
  dataInicio?: string;   // Opcional
  endDate?: string;      // Opcional
  dataFim?: string;      // Opcional
  
  voteType?: VoteType;
  votePrivacy?: VotePrivacy;
  options?: { id: string; label: string }[];
  votes?: any[];
  pollVotes?: any[];
  pollOptions?: any[];
  
  // Links e Anexos
  linkVideoConferencia?: string;
  youtubeLiveUrl?: string; 
  relatorioIaUrl?: string;
  anexoUrl?: string;     // Visto no log anterior
  attachments?: string[]; 
}

export interface Poll {
  id: string;
  title: string;
  description: string;
  status: 'OPEN' | 'CLOSED';
  targetAudience: 'ALL' | 'COUNCIL';
  votes: { userId: string; optionId: string }[];
  options: { id: string; label: string }[];
  endDate: string;
}

// --- Chat em Tempo Real ---
export interface ChatMessage {
  id?: string;
  assemblyId: string;
  userId: string;
  userName: string;
  senderName?: string; // Compatibilidade
  content: string;
  timestamp?: string;
  createdAt?: string;
  role?: string;
}

// --- Comunicação e Avisos ---
export interface Announcement {
  id: string;
  title: string;
  content: string;
  date: string;
  priority: 'HIGH' | 'NORMAL' | 'LOW';
  targetType: 'ALL' | 'BLOCK' | 'UNIT' | 'GROUP';
  targetValue?: string;
  readBy: string[];
  requiresConfirmation: boolean;
}

// --- Áreas Comuns e Reservas ---
export interface CommonArea {
  id: string;
  name: string;
  capacity: number;
  price: number;
  imageUrl: string;
  openTime: string;
  closeTime: string;
  description: string;
  rules?: string;
  requiresApproval?: boolean;
}

export type BookingStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';

export interface Booking {
  id: string;
  areaId: string;
  areaName?: string; // Útil para listagem
  userId: string;
  userName?: string;
  unit: string;
  date: string;
  startTime: string;
  endTime: string;
  status: BookingStatus;
  totalPrice: number;
  createdAt?: string;
}

// --- Chamados (Tickets) ---
export enum TicketStatus {
  OPEN = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  RESOLVED = 'RESOLVED',
  CLOSED = 'CLOSED'
}

export enum TicketPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

export interface Ticket {
  id: string;
  title: string;
  description: string;
  category: string;
  status: TicketStatus;
  priority: TicketPriority;
  userId: string;
  userName?: string;
  userUnit?: string;
  createdAt: string;
  updatedAt: string;
  messages?: TicketMessage[];
  attachments?: string[];
}

export interface TicketMessage {
  id: string;
  ticketId: string;
  senderId: string;
  senderName: string;
  message: string;
  sentAt: string;
  isAdminSender: boolean;
}

// --- Governança e Auditoria ---
export interface GovernanceActivity {
  id: string;
  type: string;
  description: string;
  timestamp: string;
  userId: string;
  ipAddress?: string;
}

export interface AuditLog {
  id: string;
  timestamp: string;
  action: string;
  userId: string;
  userName?: string;
  details: string;
  resourceType?: string;
}

// --- Blog e Conteúdo ---
export interface BlogPost {
  id: string;
  title: string;
  slug: string;
  content: string;
  date: string;
  author: string;
  excerpt?: string;
  image?: string;
  category?: string;
}

// --- Financeiro (Se houver) ---
export interface FinancialTransaction {
  id: string;
  description: string;
  amount: number;
  type: 'INCOME' | 'EXPENSE';
  category: string;
  date: string;
  status: 'PAID' | 'PENDING' | 'OVERDUE';
}