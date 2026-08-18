/// <reference types="vite/client" />
import React, { useEffect, useRef } from 'react';
import { HashRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

// --- PÁGINAS PÚBLICAS ---
import LandingPage from './pages/LandingPage';
import Pricing from './pages/Pricing';
import GovernanceSales from './pages/GovernanceSales';
import Testimonials from './pages/Testimonials';
import Blog from './pages/Blog';
import Compliance from './pages/Compliance';
import FAQ from './pages/FAQ'; 
import TermsOfUse from './pages/TermsOfUse'; 
import PrivacyPolicy from './pages/PrivacyPolicy'; 

// --- AUTENTICAÇÃO E REGISTRO ---
import Auth from './pages/Auth';
import SelectContext from './pages/auth/SelectContext';
import CondoRegister from './pages/auth/CondoRegister';
import { AffiliateRegister } from './pages/auth/AffiliateRegister';
import ForgotPassword from './pages/ForgotPassword'; 

// --- DASHBOARDS ---
import SuperAdminDashboard from './pages/dashboards/SuperAdminDashboard';
import AffiliateDashboard from './pages/dashboards/AffiliateDashboard';
import Dashboard from './pages/Dashboard';

// --- FUNCIONALIDADES ---
import SubscriptionRenovation from './pages/subscription/SubscriptionRenovation'; 
import AssemblyList from './pages/AssemblyList';
import CreateAssembly from './pages/CreateAssembly';
import VotingRoom from './pages/VotingRoom';
import Governance from './pages/Governance';
import Reports from './pages/Reports';
import Spaces from './pages/Spaces'; 
import Profile from './pages/Profile'; 
import Tickets from './pages/Tickets';
import Orders from './pages/Orders'; 
import Guests from './pages/Guests'; // <-- IMPORTAÇÃO DO MÓDULO DE CONVIDADOS

// --- CHATBOT (NOVO) ---
import { SupportChatbot } from './components/SupportChatbot'; 

// --- COMPONENTE DE PROTEÇÃO DE ROTAS ---
const PrivateRoute = ({ children, allowedRoles }: { children: React.ReactNode, allowedRoles?: string[] }) => {
  const { isAuthenticated, user, loading } = useAuth();

  if (loading) return <div className="min-h-screen flex items-center justify-center bg-slate-50 text-slate-500 font-bold">Carregando Votzz...</div>;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Verifica permissão por Role
  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    // Redirecionamento inteligente baseado no cargo se tentar acessar algo proibido
    if (user.role === 'ADMIN') return <Navigate to="/admin/dashboard" replace />;
    if (user.role === 'AFILIADO') return <Navigate to="/affiliate/dashboard" replace />;
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};

const AppRoutes: React.FC = () => {
    const { isAuthenticated } = useAuth(); 
    const clientRef = useRef<Client | null>(null);

    // --- CONFIGURAÇÃO DO WEBSOCKET (STOMP) ---
    useEffect(() => {
        const token = localStorage.getItem('@Votzz:token');
        
        if (!isAuthenticated || !token) {
            if (clientRef.current) {
                clientRef.current.deactivate();
                clientRef.current = null;
            }
            return;
        }

        if (clientRef.current && clientRef.current.active) {
            return;
        }

        // Define URL do WebSocket (Prod vs Dev)
        const socketUrl = import.meta.env.VITE_API_URL 
            ? `${import.meta.env.VITE_API_URL.replace('/api', '')}/ws-votzz`
            : 'http://localhost:8080/ws-votzz';

        const client = new Client({
            webSocketFactory: () => new SockJS(socketUrl),
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: () => {
                console.log("Votzz WebSocket Conectado");
            }, 
            onStompError: (frame) => {
                console.error('Erro no Broker: ' + frame.headers['message']);
                console.error('Detalhes: ' + frame.body);
            },
            debug: (str) => {
                // Descomente para debugar websocket
                // console.log(str); 
            } 
        });

        client.activate();
        clientRef.current = client;

        return () => {
             if (clientRef.current) {
                clientRef.current.deactivate();
             }
        };
    }, [isAuthenticated]);

    return (
        <>
            <Routes>
              {/* --- ROTAS PÚBLICAS (LANDING PAGE & INSTITUCIONAL) --- */}
              <Route path="/" element={<LandingPage user={null} />} />
              <Route path="/pricing" element={<Pricing />} />
              <Route path="/governance-sales" element={<GovernanceSales user={null} />} />
              <Route path="/testimonials" element={<Testimonials />} />
              
              <Route path="/blog" element={<Blog />} />
              <Route path="/blog/:slug" element={<Blog />} /> 
              
              <Route path="/compliance" element={<Compliance />} />
              <Route path="/faq" element={<FAQ />} />              
              <Route path="/terms" element={<TermsOfUse />} />     
              <Route path="/privacy" element={<PrivacyPolicy />} />
              
              {/* --- AUTENTICAÇÃO --- */}
              <Route path="/login" element={<Auth />} />
              <Route path="/select-context" element={<SelectContext />} />
              <Route path="/affiliate/login" element={<Auth />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              
              {/* --- REGISTROS --- */}
              <Route path="/auth/condo-register" element={<CondoRegister />} />
              <Route path="/register-condo" element={<CondoRegister />} />
              <Route path="/affiliate/register" element={<AffiliateRegister />} />

              {/* --- ÁREA LOGADA (CONDOMÍNIO) --- */}
              
              {/* Dashboard Principal (Morador + Gestor) */}
              <Route path="/dashboard" element={
                <PrivateRoute allowedRoles={['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']}>
                  <Layout><Dashboard /></Layout>
                </PrivateRoute>
              } />

              {/* Funcionalidade: Assembleias */}
              <Route path="/assemblies" element={
                <PrivateRoute allowedRoles={['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']}>
                  <Layout><AssemblyList /></Layout>
                </PrivateRoute>
              } />

              <Route path="/create-assembly" element={
                <PrivateRoute allowedRoles={['SINDICO', 'MANAGER', 'ADM_CONDO']}>
                  <Layout><CreateAssembly /></Layout>
                </PrivateRoute>
              } />

              <Route path="/voting-room/:id" element={
                <PrivateRoute allowedRoles={['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']}>
                  <Layout><VotingRoom /></Layout> 
                </PrivateRoute>
              } />

              {/* Funcionalidade: Governança (Comunicados/Enquetes) */}
              <Route path="/governance" element={
                <PrivateRoute allowedRoles={['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']}>
                  <Layout><Governance /></Layout> 
                </PrivateRoute>
              } />

              {/* Funcionalidade: Espaços */}
              <Route path="/spaces" element={
                <PrivateRoute allowedRoles={['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']}>
                  <Layout><Spaces /></Layout>
                </PrivateRoute>
              } />

              {/* Funcionalidade: Chamados */}
              <Route path="/tickets" element={
                <PrivateRoute allowedRoles={['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']}>
                    <Layout><Tickets /></Layout>
                </PrivateRoute>
              } />

              {/* Funcionalidade: Encomendas */}
              <Route path="/orders" element={
                <PrivateRoute allowedRoles={['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER', 'PORTEIRO']}>
                    <Layout><Orders /></Layout>
                </PrivateRoute>
              } />

              {/* Funcionalidade: Convidados (NOVA ROTA) */}
              <Route path="/guests" element={
                <PrivateRoute allowedRoles={['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER', 'PORTEIRO']}>
                    <Layout><Guests /></Layout>
                </PrivateRoute>
              } />

              {/* Perfil de Usuário */}
              <Route path="/profile" element={
                <PrivateRoute allowedRoles={['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER', 'ADMIN', 'AFILIADO']}>
                  <Layout><Profile /></Layout>
                </PrivateRoute>
              } />

              {/* Relatórios Financeiros (Apenas Gestores) */}
              <Route path="/reports" element={
                <PrivateRoute allowedRoles={['SINDICO', 'MANAGER', 'ADM_CONDO']}>
                  <Layout><Reports /></Layout>
                </PrivateRoute>
              } />

              {/* Renovação de Assinatura */}
              <Route path="/subscription/renew" element={
                <PrivateRoute allowedRoles={['SINDICO', 'MANAGER', 'ADM_CONDO']}>
                    <Layout><SubscriptionRenovation /></Layout>
                </PrivateRoute>
              } />

              {/* --- ÁREA LOGADA (ADMINISTRATIVA GLOBAL) --- */}
              
              {/* Super Admin */}
              <Route path="/admin/dashboard" element={
                <PrivateRoute allowedRoles={['ADMIN']}>
                  <Layout><SuperAdminDashboard /></Layout>
                </PrivateRoute>
              } />

              {/* Afiliados */}
              <Route path="/affiliate/dashboard" element={
                <PrivateRoute allowedRoles={['AFILIADO']}>
                  <Layout><AffiliateDashboard /></Layout>
                </PrivateRoute>
              } />

              {/* Rota Padrão (404) */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>

            {/* O Chatbot fica flutuando globalmente em toda a aplicação */}
            <SupportChatbot />
        </>
    );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </Router>
  );
}

export default App;