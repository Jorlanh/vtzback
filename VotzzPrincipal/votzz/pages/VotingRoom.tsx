import React, { useEffect, useState, useRef, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, MessageSquare, FileCheck, Shield, Video, Send, Lock, Clock, FileText, CheckCircle, Gavel, Scale, Download, Eye, EyeOff, Layers, X, Trash2, Edit, Calendar 
} from 'lucide-react';
import api from '../services/api'; 
import { useAuth } from '../context/AuthContext';
import SockJS from 'sockjs-client';
import { over } from 'stompjs';

let stompClient: any = null;

const VotingRoom: React.FC = () => {
  const { user } = useAuth();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [assembly, setAssembly] = useState<any>(null);
  const [hasVoted, setHasVoted] = useState(false);
  const [voteReceipt, setVoteReceipt] = useState<string | null>(null);
  const [messages, setMessages] = useState<any[]>([]);
  
  const [myUnits, setMyUnits] = useState<string[]>([]);
  const [totalWeight, setTotalWeight] = useState(1); 

  const [showUnitModal, setShowUnitModal] = useState(false);
  const [tempSelectedUnits, setTempSelectedUnits] = useState<string[]>([]);
  const [selectedOption, setSelectedOption] = useState<string | null>(null);
  const [chatMsg, setChatMsg] = useState('');
  const [connected, setConnected] = useState(false);
  const [activeTab, setActiveTab] = useState<'VOTE' | 'MANAGE'>('VOTE');
  
  const [isNotStarted, setIsNotStarted] = useState(false);
  
  const [closing, setClosing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [summarizing, setSummarizing] = useState(false); 
  const [isDownloading, setIsDownloading] = useState(false);
  
  const chatEndRef = useRef<HTMLDivElement>(null);
  
  const isManager = user?.role === 'MANAGER' || user?.role === 'SINDICO' || user?.role === 'ADM_CONDO' || user?.role === 'ADMIN';
  const isSecret = assembly?.votePrivacy === 'SECRET';
  
  const isClosed = useMemo(() => {
      const s = (assembly?.status || '').toUpperCase();
      return ['CLOSED', 'ENCERRADA', 'FINALIZADA', 'HISTORICO'].includes(s);
  }, [assembly]);

  const canVote = (user?.role === 'MORADOR') || (user?.role === 'SINDICO') || (user?.role === 'ADM_CONDO');

  const totalVotes = assembly?.votes?.length || 0;
  const userFraction = (user as any)?.fraction || 0.0152; 
  const totalFraction = totalVotes * userFraction; 
  const myTotalFraction = userFraction * totalWeight;

  const fixUrl = (url: string) => {
    if (!url) return '';
    if (url.includes('X-Amz-Signature')) return url;

    let fixed = url;
    if (fixed.includes('votzz-storage')) {
        fixed = fixed.replace('votzz-storage', 'votzz-files-prod');
    }
    if (fixed.includes('storage.votzz.com')) {
        fixed = fixed.replace('https://storage.votzz.com', 'https://votzz-files-prod.s3.sa-east-1.amazonaws.com');
    }
    return fixed;
  };

  useEffect(() => {
    if (user) {
        const unitsFromUser = (user as any)?.unidadesList || [user?.unidade].filter(Boolean);
        
        if (unitsFromUser && unitsFromUser.length > 0) {
            setMyUnits(unitsFromUser);
            setTotalWeight(unitsFromUser.length);
            setTempSelectedUnits(unitsFromUser); 
        } else {
            setMyUnits(['Unidade Padrão']);
        }

        if (assembly && assembly.votes) {
            checkIfVoted(assembly.votes);
        }
    }
  }, [user, assembly]); 

  useEffect(() => {
    if (assembly && assembly.dataInicio) {
        const now = new Date();
        const start = new Date(assembly.dataInicio);
        
        if (!isManager && now < start) {
            setIsNotStarted(true);
        } else {
            setIsNotStarted(false);
        }
    }
  }, [assembly, isManager]);

  const checkIfVoted = (votes: any[]) => {
      if (!votes || !user) return;
      const myVote = votes.find((v: any) => {
          const voteUserId = v.userId || (v.user && v.user.id);
          return voteUserId === user.id;
      });

      if (myVote) {
          setHasVoted(true);
          setVoteReceipt(myVote.id || myVote.hash);
      } else {
          setHasVoted(false);
          setVoteReceipt(null);
      }
  };

  const ataPreview = useMemo(() => {
    if (!assembly) return '';

    const votes = assembly.votes || [];
    
    const activeOptions = assembly.options && assembly.options.length > 0 
      ? assembly.options 
      : [
            { id: 'sim', descricao: 'Sim' },
            { id: 'nao', descricao: 'Não' },
            { id: 'abstencao', descricao: 'Abstenção' }
        ];

    const results = activeOptions.map((opt: any) => {
        const count = votes.filter((v: any) => v.optionId === opt.id || v.optionId === opt.descricao).length;
        const fractionPercent = ((count * userFraction) * 100).toFixed(4);
        return {
            name: opt.descricao || opt.label,
            count: count,
            percent: fractionPercent
        };
    });

    const resultsText = results.map((r: any) => 
        `- ${r.name}: ${r.count} votos (${r.percent}% da fração ideal)`
    ).join('\n');

    const sortedResults = [...results].sort((a: any, b: any) => b.count - a.count);
    
    let decision = "EMPATADO";
    if (totalVotes === 0) {
        decision = "SEM QUÓRUM para deliberação";
    } else if (sortedResults[0].count > (sortedResults[1]?.count || 0)) {
        decision = `APROVADA a opção: ${sortedResults[0].name}`;
    }

    const date = new Date();
    const formattedDate = date.toLocaleDateString('pt-BR');
    
    return `ATA DA ASSEMBLEIA GERAL AGE DO CONDOMÍNIO (ID: ${assembly.id.substring(0, 8)})

Aos ${formattedDate}, encerrou-se a votação eletrônica referente à convocação enviada.

A assembleia foi realizada na modalidade VIRTUAL, em conformidade com o Art. 1.354-A do Código Civil Brasileiro.

1. DA CONVOCAÇÃO
Foi comprovado o envio de notificação aos condôminos via sistema Votzz.

2. DO QUÓRUM
Estiveram presentes (votantes) ${totalVotes} unidades, representando uma fração ideal total de ${(totalFraction * 100).toFixed(4)}%.

3. DA ORDEM DO DIA: "${assembly.titulo}"
${assembly.description}

4. DA VOTAÇÃO
O sistema registrou votos criptografados e auditáveis, com o seguinte resultado:
${resultsText}

5. DA DELIBERAÇÃO
Com base nos votos válidos, foi ${decision}.

A presente ata é gerada automaticamente pelo sistema Votzz, com hash de integridade SHA-256.





________________________________________________
Assinado digitalmente pelo Presidente da Mesa / Síndico.`;
  }, [assembly, totalVotes, totalFraction]);

  useEffect(() => {
    let isMounted = true;

    const connectWebSocket = () => {
        if (stompClient && stompClient.connected) {
            if (isMounted) setConnected(true);
            return;
        }

        const apiUrl = (import.meta as any).env.VITE_API_URL || 'http://localhost:8080/api';
        const baseUrl = apiUrl.replace(/\/api$/, ''); 
        const socketUrl = `${baseUrl}/ws-votzz`;

        const socket = new SockJS(socketUrl);
        stompClient = over(socket);
        stompClient.debug = () => {}; 

        stompClient.connect(
            {}, 
            () => { 
                if (!isMounted) return;
                setConnected(true);
                console.log("WebSocket Conectado com sucesso.");
                
                stompClient.subscribe(`/topic/assembly/${id}`, (message: any) => {
                    if (message.body) {
                        const newMsg = JSON.parse(message.body);
                        
                        if (!newMsg.type || newMsg.type === 'CHAT') {
                            setMessages(prev => [...prev, newMsg]);
                            setTimeout(() => chatEndRef.current?.scrollIntoView({ behavior: "smooth" }), 100);
                        }
                        if (newMsg.type === 'STATUS_UPDATE') {
                            setAssembly((prev: any) => ({ ...prev, status: newMsg.status }));
                        }
                    }
                });
            },
            (error: any) => { 
                console.error("Erro na conexão WebSocket:", error);
                if (isMounted) {
                    setConnected(false);
                    setTimeout(connectWebSocket, 5000);
                }
            }
        );
    };

    if (id && user) {
        connectWebSocket();
    }

    return () => {
        isMounted = false;
        if (stompClient && stompClient.connected) {
            stompClient.disconnect(() => console.log("WebSocket Desconectado"));
        }
    };
  }, [id, user]);

  useEffect(() => {
    loadData();
  }, [id]); 

  const loadData = async () => {
    if (!id) return;
    
    try {
        const resAssembly = await api.get(`/assemblies/${id}`);
        setAssembly(resAssembly.data);
    } catch (e) {
        console.error("Erro crítico ao carregar assembleia:", e);
    }

    try {
        const resChat = await api.get(`/chat/assemblies/${id}`);
        if (Array.isArray(resChat.data)) {
            setMessages(resChat.data);
            setTimeout(() => chatEndRef.current?.scrollIntoView(), 500);
        } else {
            setMessages([]);
        }
    } catch (e: any) {
        if (e.response && e.response.status !== 400) {
             console.warn("Histórico de chat indisponível:", e);
        }
        setMessages([]); 
    }
  };

  const handleVoteClick = () => {
    if (!id || !selectedOption || !user) return;
    if (myUnits.length > 1) {
        if (tempSelectedUnits.length === 0) setTempSelectedUnits(myUnits);
        setShowUnitModal(true);
    } else {
        submitFinalVote(myUnits);
    }
  };

  const submitFinalVote = async (unitsToVote: string[]) => {
    try {
      if (unitsToVote.length === 0) {
        alert("Selecione pelo menos uma unidade para votar.");
        return;
      }

      if (!selectedOption) {
        alert("Selecione uma opção para votar.");
        return;
      }

      const payload = { 
        optionId: selectedOption, 
        userId: user?.id,
        units: unitsToVote 
      };

      const response = await api.post(`/assemblies/${id}/vote`, payload);
      
      setHasVoted(true);
      setVoteReceipt(response.data.id || 'CONFIRMADO-MULTI');
      setShowUnitModal(false);
      setTotalWeight(unitsToVote.length); 
      
      alert(`Voto registrado com sucesso para ${unitsToVote.length} unidade(s)!`);
      loadData();
    } catch (e: any) { 
        console.error("Erro ao votar:", e);
        const errorMsg = e.response?.data?.message || e.response?.data?.error || "Erro desconhecido ao processar voto.";
        alert("Erro ao votar: " + errorMsg); 
    }
  };

  const handleSendChat = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatMsg.trim() || !user) return;

    if (!stompClient || !stompClient.connected) {
        alert("Conexão com o chat perdida. Aguarde a reconexão...");
        return;
    }

    const chatDTO = {
      senderName: user.nome || user.name || user.email,
      content: chatMsg,
      userId: user.id,
      tenantId: user.tenantId, 
      assemblyId: id,
      type: 'CHAT'
    };

    try {
        stompClient.send(`/app/chat/${id}/send`, {}, JSON.stringify(chatDTO));
        setChatMsg('');
    } catch (err) {
        console.error("Erro ao enviar mensagem:", err);
    }
  };

  const handleCloseAssembly = async () => {
    if (!id || !window.confirm("ATENÇÃO: Isso encerrará a votação e gerará a Ata Jurídica. Confirmar?")) return;
    setClosing(true);
    try {
        await api.patch(`/assemblies/${id}/close`); 
        loadData();
    } catch(e: any) {
        alert("Erro ao encerrar: " + (e.response?.data?.message || e.message));
    } finally {
        setClosing(false);
    }
  };

  const handleDeleteAssembly = async () => {
      if(!id || !window.confirm("TEM CERTEZA? Isso apagará a assembleia e todo o histórico de votos e chat permanentemente. Esta ação não pode ser desfeita.")) return;
      setDeleting(true);
      try {
          await api.delete(`/assemblies/${id}`);
          alert("Assembleia excluída com sucesso.");
          navigate('/assemblies');
      } catch (error: any) {
          alert("Erro ao excluir: " + (error.response?.data?.message || "Erro desconhecido"));
          setDeleting(false);
      }
  };

  const handleEditAssembly = () => {
      navigate('/create-assembly', { state: { assemblyData: assembly } });
  };

  const handleExportDossier = async () => {
      if(!id || !assembly) return;
      
      setIsDownloading(true); 

      try {
          console.log("Iniciando download seguro do Dossiê...");

          const response = await api.get(`/assemblies/${id}/dossier`, {
              responseType: 'blob' 
          });

          // Nome do arquivo customizado
          const tenantName = assembly.tenant?.nome ? assembly.tenant.nome.replace(/[^a-zA-Z0-9]/g, '_') : 'Condominio';
          const title = assembly.titulo ? assembly.titulo.replace(/[^a-zA-Z0-9]/g, '_') : 'Assembleia';
          const code = id.substring(0,8);
          const fileName = `${tenantName}_${title}_${code}.pdf`;

          const blob = new Blob([response.data], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          
          const link = document.createElement('a');
          link.href = url;
          link.setAttribute('download', fileName); 
          document.body.appendChild(link);
          link.click();
          
          link.parentNode?.removeChild(link);
          window.URL.revokeObjectURL(url);

          console.log("Download do Dossiê concluído.");

      } catch (error: any) {
          console.error("Erro ao baixar dossiê:", error);
          if (error.response?.status === 403) {
              alert("Acesso negado. Verifique se você é o síndico.");
          } else {
              alert("Erro ao gerar o documento. Tente novamente.");
          }
      } finally {
          setIsDownloading(false);
      }
  };

  const handlePrintAta = () => {
      if (!assembly || !ataPreview) return;

      const cleanString = (str: string) => str ? str.replace(/[^a-zA-Z0-9]/g, '_') : 'Desconhecido';
      
      const assembleiaNome = cleanString(assembly.titulo || assembly.title);
      const condominioNome = cleanString(assembly.tenant?.nome || 'Condominio');
      
      const fileName = `DossieJuridicoAssemblyVotzz_${assembleiaNome}_${condominioNome}`;

      const printWindow = window.open('', '_blank', 'width=900,height=800');
      
      if (printWindow) {
          printWindow.document.write(`
            <html>
            <head>
                <title>${fileName}</title>
                <style>
                    body { font-family: 'Courier New', Courier, monospace; padding: 40px; line-height: 1.6; font-size: 12px; color: #000; max-width: 800px; margin: 0 auto; }
                    .header { text-align: center; margin-bottom: 40px; border-bottom: 2px solid #000; padding-bottom: 20px; }
                    .logo { font-weight: bold; font-size: 20px; margin-bottom: 10px; }
                    .content { white-space: pre-wrap; text-align: justify; }
                    .footer { margin-top: 50px; text-align: center; font-size: 10px; color: #666; border-top: 1px solid #ccc; padding-top: 10px; }
                    @media print { body { -webkit-print-color-adjust: exact; } button { display: none; } }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="logo">Votzz - Sistema de Governança</div>
                    <div>Registro Digital de Assembleia</div>
                </div>
                
                <div class="content">${ataPreview}</div>

                <div class="footer">
                    Documento gerado eletronicamente em ${new Date().toLocaleString()} <br/>
                    Válido para: ${assembly.tenant?.nome || 'Condomínio'} <br/>
                    Hash de integridade do sistema Votzz.
                </div>

                <script>
                    window.onload = function() { 
                        setTimeout(function() { window.print(); }, 500); 
                    }
                </script>
            </body>
            </html>
          `);
          printWindow.document.close();
      } else {
          alert("Por favor, permita pop-ups para baixar o PDF.");
      }
  };

  const handleDownloadFile = async () => {
    if (!assembly || !assembly.anexoUrl) return;
    
    setIsDownloading(true);
    const correctedUrl = fixUrl(assembly.anexoUrl);
    
    try {
        console.log("Tentando baixar arquivo via Fetch/Blob em:", correctedUrl);
        
        const response = await fetch(correctedUrl, {
            method: 'GET',
            mode: 'cors', 
            headers: { 'Cache-Control': 'no-cache' }
        });

        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }

        const blob = await response.blob();
        const blobUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = blobUrl;
        
        const fileName = correctedUrl.split('/').pop() || 'documento_anexo.pdf';
        link.setAttribute('download', fileName);
        
        document.body.appendChild(link);
        link.click();
        
        link.parentNode?.removeChild(link);
        window.URL.revokeObjectURL(blobUrl);

    } catch (error) {
        console.warn("Fetch falhou ou foi bloqueado. Tentando abrir direto...", error);
        window.open(correctedUrl, '_blank', 'noopener,noreferrer');
    } finally {
        setIsDownloading(false);
    }
  };

  const handleSummarizeIA = () => {
      if(!id) return;
      setSummarizing(true);
      const apiUrl = (import.meta as any).env.VITE_API_URL || 'http://localhost:8080/api';
      window.open(`${apiUrl}/chat/assemblies/${id}/resumo-pdf`, '_blank');
      setTimeout(() => setSummarizing(false), 2000);
  };

  const getYoutubeEmbedUrl = (url: string) => {
    if (!url) return "";
    const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=|live\/)([^#\&\?]*).*/;
    const match = url.match(regExp);
    if (match && match[2].length === 11) {
        return `https://www.youtube.com/embed/${match[2]}`;
    }
    return "";
  };

  if (!assembly) return (
      <div className="p-20 text-center animate-pulse flex flex-col items-center justify-center">
          <Clock className="w-12 h-12 text-slate-300 mb-4 animate-spin"/>
          <p className="text-slate-400 font-bold">Carregando sala de votação...</p>
      </div>
  );

  if (isNotStarted) {
      return (
          <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
            <div className="bg-white max-w-md w-full rounded-2xl shadow-lg p-8 text-center border border-slate-200">
              <div className="w-16 h-16 bg-amber-50 rounded-full flex items-center justify-center mx-auto mb-6">
                 <Clock className="text-amber-500 w-8 h-8" />
              </div>
              <h2 className="text-xl font-bold text-slate-800 mb-2">Sala de Votação Fechada</h2>
              <p className="text-slate-500 text-sm mb-6">
                Esta assembleia ainda não começou. O acesso será liberado automaticamente no horário agendado.
              </p>
              <div className="bg-slate-50 rounded-xl p-4 mb-6 border border-slate-200">
                <p className="text-xs text-slate-400 font-bold uppercase tracking-widest mb-1">Início Programado</p>
                <div className="text-lg font-bold text-slate-700 flex items-center justify-center gap-2">
                  <Calendar size={18} className="text-emerald-600"/>
                  {new Date(assembly.dataInicio).toLocaleString('pt-BR')}
                </div>
              </div>
              <button onClick={() => navigate('/dashboard')} className="w-full bg-slate-800 text-white py-3 rounded-lg font-bold hover:bg-slate-900 transition-all">Voltar ao Painel</button>
            </div>
          </div>
      );
  }

  const votingOptions = assembly.options && assembly.options.length > 0 
      ? assembly.options 
      : [
            { id: 'sim', descricao: 'Sim' },
            { id: 'nao', descricao: 'Não' },
            { id: 'abstencao', descricao: 'Abstenção' }
        ];

  const toggleUnit = (unit: string) => {
    setTempSelectedUnits(prev => prev.includes(unit) ? prev.filter(u => u !== unit) : [...prev, unit]);
  };

  return (
    <div className="space-y-6 pb-20 p-4 md:p-6 max-w-7xl mx-auto">
      
      <div className="flex justify-between items-center">
        <button onClick={() => navigate('/assemblies')} className="flex items-center text-slate-500 hover:text-slate-800 transition-colors">
            <ArrowLeft className="h-4 w-4 mr-1" /> Voltar para lista
        </button>
        
        {isManager && (
            <div className="flex bg-slate-100 p-1 rounded-lg">
                <button 
                    onClick={() => setActiveTab('VOTE')} 
                    className={`px-4 py-1.5 text-sm font-bold rounded-md transition-all ${activeTab === 'VOTE' ? 'bg-white shadow text-slate-800' : 'text-slate-500'}`}
                >
                  Sala de Votação
                </button>
                <button 
                    onClick={() => setActiveTab('MANAGE')} 
                    className={`px-4 py-1.5 text-sm font-bold rounded-md transition-all ${activeTab === 'MANAGE' ? 'bg-white shadow text-slate-800' : 'text-slate-500'}`}
                >
                  Gestão & Encerramento
                </button>
            </div>
        )}
      </div>

      {activeTab === 'MANAGE' && isManager ? (
          <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
              <div className="bg-slate-900 text-white p-8 rounded-[2rem] shadow-xl relative overflow-hidden">
                <div className="relative z-10 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                    <div>
                        <h2 className="text-2xl font-black flex items-center gap-2">
                            <Gavel className="w-6 h-6 text-emerald-400" /> Painel Legal do Síndico
                        </h2>
                        <p className="text-slate-400 text-sm mt-1">Controle de quórum e formalização jurídica (Art. 1.354-A CC).</p>
                    </div>
                    <div className="flex items-center gap-3">
                        <span className={`px-4 py-1.5 rounded-full text-xs font-black uppercase tracking-widest ${isClosed ? 'bg-red-500 text-white' : 'bg-emerald-500 text-white animate-pulse'}`}>
                            {isClosed ? 'ENCERRADA' : 'EM ANDAMENTO'}
                        </span>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-8 relative z-10">
                    <div className="bg-white/10 backdrop-blur-sm p-5 rounded-2xl border border-white/10">
                        <p className="text-slate-400 text-[10px] uppercase font-black tracking-widest">Quórum (Unidades)</p>
                        <p className="text-3xl font-black mt-1">{totalVotes} <span className="text-sm font-medium text-slate-400">presentes</span></p>
                    </div>
                    <div className="bg-white/10 backdrop-blur-sm p-5 rounded-2xl border border-white/10">
                          <p className="text-slate-400 text-[10px] uppercase font-black tracking-widest">Fração Ideal Total</p>
                          <p className="text-3xl font-black mt-1">{(totalFraction * 100).toFixed(2)}%</p>
                    </div>
                    <div className="bg-white/10 backdrop-blur-sm p-5 rounded-2xl border border-white/10">
                          <p className="text-slate-400 text-[10px] uppercase font-black tracking-widest">Convocação</p>
                          <p className="text-3xl font-black mt-1 text-emerald-400">100% <span className="text-sm font-medium text-slate-400">entregue</span></p>
                    </div>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-6">
                  {!isClosed ? (
                    <div className="bg-white p-8 rounded-[2rem] border border-slate-100 shadow-sm space-y-6">
                        <h3 className="font-black text-lg text-slate-800 mb-2">Ações Administrativas</h3>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <button 
                                onClick={handleEditAssembly} 
                                className="w-full bg-amber-100 hover:bg-amber-200 text-amber-800 px-6 py-4 rounded-xl font-bold flex items-center justify-center gap-2 transition-all border border-amber-200"
                            >
                                <Edit className="w-5 h-5" /> Editar Dados
                            </button>
                            <button 
                                onClick={handleDeleteAssembly} 
                                disabled={deleting}
                                className="w-full bg-red-100 hover:bg-red-200 text-red-800 px-6 py-4 rounded-xl font-bold flex items-center justify-center gap-2 transition-all border border-red-200"
                            >
                                {deleting ? 'Apagando...' : <><Trash2 className="w-5 h-5" /> Excluir Assembleia</>}
                            </button>
                        </div>

                        <hr className="border-slate-100" />

                        <div>
                            <h4 className="font-bold text-slate-800 mb-2">Finalização</h4>
                            <p className="text-sm text-slate-500 mb-4">Ao encerrar, o sistema calculará os votos ponderados pela fração ideal e gerará a Ata automaticamente.</p>
                            <button onClick={handleCloseAssembly} disabled={closing} className="w-full bg-slate-800 hover:bg-slate-900 text-white px-6 py-4 rounded-xl font-black flex items-center justify-center gap-2 transition-all shadow-lg shadow-slate-200 disabled:opacity-50">
                                {closing ? 'Processando...' : <><Lock className="w-5 h-5" /> Encerrar Votação e Lavrar Ata</>}
                            </button>
                        </div>
                    </div>
                  ) : (
                    <div className="space-y-6">
                        <div className="bg-emerald-50 p-6 rounded-[2rem] border border-emerald-100 shadow-sm flex items-center gap-4">
                            <div className="bg-emerald-100 p-3 rounded-full">
                                <CheckCircle className="w-6 h-6 text-emerald-600"/>
                            </div>
                            <div>
                                <h3 className="font-black text-lg text-emerald-800">Ata Gerada com Sucesso</h3>
                                <p className="text-sm text-emerald-600">A votação foi auditada e o documento oficial foi criado.</p>
                            </div>
                        </div>

                        <div className="bg-white p-6 rounded-[1rem] shadow-inner border border-slate-200 font-mono text-xs leading-relaxed text-slate-700 overflow-y-auto max-h-96 whitespace-pre-wrap">
                            {ataPreview}
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <button onClick={handlePrintAta} className="w-full bg-slate-800 text-white px-6 py-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-slate-900 transition-all shadow-lg">
                                <FileText className="w-5 h-5" /> Baixar Ata (PDF)
                            </button>
                            <button onClick={handleExportDossier} className="w-full bg-slate-700 text-white px-6 py-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-slate-800 transition-all border border-slate-600">
                                <Shield className="w-5 h-5" /> Exportar Dossiê Jurídico
                            </button>
                        </div>

                        <button onClick={handleDeleteAssembly} className="w-full mt-4 text-red-500 hover:text-red-700 text-sm font-bold flex items-center justify-center gap-2">
                            <Trash2 size={16}/> Apagar do Histórico
                        </button>
                    </div>
                  )}
              </div>
          </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 animate-in fade-in">
            
            <div className="lg:col-span-8 space-y-6">
                
                <div className="bg-white p-6 rounded-[2rem] border border-slate-100 shadow-sm flex justify-between items-center">
                    <div>
                        <h1 className="text-2xl font-black text-slate-800">{assembly.titulo || assembly.title}</h1>
                        <div className="flex items-center gap-3 mt-1">
                            <span className="text-xs font-bold bg-slate-100 text-slate-500 px-2 py-1 rounded uppercase tracking-wide">{assembly.type || assembly.tipoAssembleia}</span>
                            <span className={`text-xs font-bold px-2 py-1 rounded uppercase tracking-wide ${isClosed ? 'bg-red-100 text-red-700' : 'bg-emerald-100 text-emerald-700'}`}>
                                {isClosed ? 'Encerrada' : 'Aberta para Votos'}
                            </span>
                        </div>
                    </div>
                    <div className="flex flex-col items-end">
                        <div className="flex items-center gap-2 text-xs font-bold text-slate-400 uppercase tracking-widest">
                            {isSecret ? <><EyeOff size={14}/> Voto Secreto</> : <><Eye size={14}/> Voto Aberto</>}
                        </div>
                        {connected ? (
                            <span className="text-[10px] font-black text-emerald-500 uppercase flex items-center gap-1 mt-1"><span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"/> Conectado</span>
                        ) : (
                            <span className="text-[10px] font-black text-red-500 uppercase flex items-center gap-1 mt-1"><span className="w-2 h-2 bg-red-500 rounded-full"/> Offline</span>
                        )}
                    </div>
                </div>

                {assembly.youtubeLiveUrl ? (
                    <div className="aspect-video bg-black rounded-[2rem] overflow-hidden shadow-2xl border-4 border-slate-900 relative group">
                        <iframe 
                            width="100%" height="100%" 
                            src={`${getYoutubeEmbedUrl(assembly.youtubeLiveUrl)}?autoplay=1&mute=0`} 
                            title="Live" frameBorder="0" allowFullScreen
                            className="absolute inset-0 w-full h-full"
                        ></iframe>
                    </div>
                ) : (
                    <div className="aspect-video bg-slate-100 rounded-[2rem] flex flex-col items-center justify-center text-slate-400 border-2 border-dashed border-slate-200">
                        <Video size={48} className="mb-4 opacity-20"/>
                        <p className="font-bold">Nenhum link de transmissão configurado.</p>
                    </div>
                )}

                <div className="bg-white p-8 rounded-[2rem] border border-slate-100 shadow-sm">
                    <h3 className="text-slate-400 font-black uppercase text-xs tracking-widest mb-4 flex items-center gap-2"><FileText size={16}/> Pauta Oficial</h3>
                    <div className="prose prose-slate max-w-none text-slate-700 leading-relaxed whitespace-pre-wrap">
                        {assembly.description}
                    </div>
                    
                    {assembly.anexoUrl && (
                        <div className="mt-8 pt-6 border-t border-slate-100">
                            <h4 className="text-xs font-black text-slate-400 uppercase mb-3">Documentos Anexos</h4>
                            <div 
                                onClick={handleDownloadFile}
                                className={`flex items-center gap-3 p-4 bg-slate-50 border border-slate-200 rounded-xl hover:bg-slate-100 transition-colors group cursor-pointer ${isDownloading ? 'opacity-70 cursor-wait' : ''}`}
                            >
                                <div className="bg-white p-2 rounded-lg border border-slate-200 text-red-500 relative">
                                    <FileText size={20}/>
                                    {isDownloading && <span className="absolute inset-0 flex items-center justify-center bg-white/80 rounded-lg"><Clock size={12} className="animate-spin text-slate-800"/></span>}
                                </div>
                                <div className="flex-1">
                                    <span className="font-bold text-slate-700 text-sm group-hover:text-blue-600 block">Documento da Assembleia (PDF)</span>
                                    <span className="text-[10px] text-slate-400">{isDownloading ? 'Preparando download...' : 'Clique para visualizar ou baixar'}</span>
                                </div>
                                <Download size={16} className={`ml-auto text-slate-400 ${isDownloading ? 'animate-bounce' : ''}`}/>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            <div className="lg:col-span-4 space-y-6">
                
                <div className="bg-white rounded-xl shadow-sm border border-slate-100 p-6 sticky top-6">
                    <h3 className="text-lg font-bold text-slate-800 mb-2 flex items-center">
                        <Lock className="h-5 w-5 mr-2 text-emerald-600" />
                        Cédula de Votação
                    </h3>

                    <div className="bg-slate-50 p-3 rounded-lg mb-4 border border-slate-200">
                        <p className="text-xs text-slate-500 uppercase font-bold mb-1 flex justify-between">
                            <span>Seu Poder de Voto</span>
                            {totalWeight > 1 && <span className="text-emerald-600 flex items-center gap-1"><Layers size={10}/> {totalWeight} Unidades</span>}
                        </p>
                        
                        <div className="flex flex-wrap gap-1 mb-2">
                            {myUnits.map((u, i) => (
                                <span key={i} className="text-[10px] bg-white border px-1.5 py-0.5 rounded text-slate-600 font-mono">{u}</span>
                            ))}
                        </div>

                        <div className="flex justify-between items-center border-t border-slate-200 pt-2">
                            <span className="text-sm font-medium text-slate-700">Fração Total:</span>
                            <span className="bg-emerald-100 text-emerald-800 text-xs px-2 py-1 rounded font-bold flex items-center">
                                <Scale className="w-3 h-3 mr-1" /> {(myTotalFraction * 100).toFixed(4)}%
                            </span>
                        </div>
                    </div>

                    {isSecret ? (
                        <div className="flex items-center text-xs text-purple-700 bg-purple-50 p-2 rounded mb-4">
                            <EyeOff className="w-3 h-3 mr-1" /> Votação Secreta.
                        </div>
                    ) : (
                        <div className="flex items-center text-xs text-blue-700 bg-blue-50 p-2 rounded mb-4">
                            <Eye className="w-3 h-3 mr-1" /> Voto Aberto.
                        </div>
                    )}

                    {hasVoted ? (
                        <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-4 text-center">
                            <div className="h-12 w-12 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-3">
                                <CheckCircle className="h-6 w-6 text-emerald-600" />
                            </div>
                            <h4 className="font-bold text-emerald-900">Voto Confirmado</h4>
                            <p className="text-sm text-emerald-700 mt-1 mb-3">Obrigado por participar.</p>
                            <div className="text-xs bg-white p-2 rounded border border-emerald-100 text-slate-500 break-all font-mono">
                                Receipt: {voteReceipt}
                            </div>
                        </div>
                    ) : isClosed ? (
                        <div className="bg-slate-100 border border-slate-200 rounded-lg p-4 text-center text-slate-500">
                            <Lock className="h-8 w-8 mx-auto mb-2 opacity-50" />
                            <p className="font-bold">Votação Encerrada</p>
                        </div>
                    ) : canVote ? (
                        <div className="space-y-3">
                            {votingOptions.map((opt: any) => (
                                <button
                                    key={opt.id || opt.descricao}
                                    onClick={() => setSelectedOption(opt.id || opt.descricao)}
                                    className={`w-full text-left px-4 py-3 rounded-lg border transition-all ${
                                        selectedOption === (opt.id || opt.descricao)
                                        ? 'border-emerald-500 ring-1 ring-emerald-500 bg-emerald-50 text-emerald-900' 
                                        : 'border-slate-200 hover:border-slate-300 text-slate-700'
                                    }`}
                                >
                                    <span className="font-medium">{opt.descricao || opt.label}</span>
                                </button>
                            ))}
                            <button
                                onClick={handleVoteClick}
                                disabled={!selectedOption}
                                className="w-full mt-6 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 text-white font-bold py-3 rounded-lg shadow-md transition-colors"
                            >
                                {myUnits.length > 1 ? 'Selecionar Unidades & Confirmar' : 'Confirmar Voto Seguro'}
                            </button>
                        </div>
                    ) : (
                        <div className="text-center p-6 bg-slate-50 rounded-2xl border border-slate-200 text-slate-400">
                            <Shield className="mx-auto mb-2 opacity-50" size={32}/>
                            <p className="font-bold text-sm">Apenas moradores habilitados podem votar.</p>
                        </div>
                    )}
                    
                    <div className="mt-6 pt-6 border-t border-slate-100">
                        <div className="flex items-start text-xs text-slate-500">
                            <FileCheck className="h-4 w-4 mr-2 flex-shrink-0 text-slate-400" />
                            <p>Em conformidade com Art. 1.354-A do CC. Hash auditável e imutável.</p>
                        </div>
                    </div>
                </div>

                <div className="bg-white rounded-[2rem] border shadow-lg overflow-hidden flex flex-col h-[500px]">
                    <div className="p-4 bg-slate-50 border-b flex justify-between items-center">
                        <span className="font-black text-slate-700 flex items-center gap-2"><MessageSquare size={18}/> Chat</span>
                        <div className="flex items-center gap-1">
                            <span className={`w-2 h-2 rounded-full ${isClosed ? 'bg-red-500' : 'bg-emerald-500 animate-pulse'}`}></span>
                            <span className="text-[10px] font-bold text-slate-400 uppercase">{isClosed ? 'Histórico' : 'Ao Vivo'}</span>
                        </div>
                    </div>
                    
                    <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-slate-50/30">
                        {messages.length === 0 && (
                            <div className="text-center mt-20 opacity-30">
                                <MessageSquare size={48} className="mx-auto mb-2"/>
                                <p className="font-bold text-xs">Nenhuma mensagem.</p>
                            </div>
                        )}
                        {messages.map((m, idx) => (
                            <div key={idx} className={`flex flex-col ${m.userId === user?.id ? 'items-end' : 'items-start'}`}>
                                <div className="flex items-center gap-2 mb-1">
                                    <span className="text-[10px] font-black text-slate-400 uppercase">{m.senderName}</span>
                                    <span className="text-[9px] text-slate-300">
                                        {new Date(m.timestamp || Date.now()).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                                    </span>
                                </div>
                                <div className={`p-3 rounded-2xl max-w-[90%] text-sm shadow-sm ${m.userId === user?.id ? 'bg-blue-600 text-white rounded-tr-none' : 'bg-white border border-slate-200 text-slate-700 rounded-tl-none'}`}>
                                    {m.content}
                                </div>
                            </div>
                        ))}
                        <div ref={chatEndRef} />
                    </div>

                    <form onSubmit={handleSendChat} className="p-3 bg-white border-t flex gap-2 items-center">
                        <input 
                            value={chatMsg} 
                            onChange={e => setChatMsg(e.target.value)} 
                            placeholder={isClosed ? "O chat foi encerrado." : "Escreva sua mensagem..."}
                            className="flex-1 bg-slate-100 border-none rounded-xl px-4 py-3 text-sm font-medium outline-none focus:ring-2 focus:ring-blue-500 transition-all disabled:opacity-70" 
                            disabled={isClosed}
                        />
                        <button type="submit" disabled={!chatMsg.trim() || !connected || isClosed} className="p-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 disabled:opacity-50 transition-colors shadow-lg shadow-blue-200">
                            <Send size={18}/>
                        </button>
                    </form>
                </div>
            </div>
        </div>
      )}

      {showUnitModal && (
        <div className="fixed inset-0 bg-black/60 z-[100] flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in">
          <div className="bg-white rounded-[2.5rem] p-8 max-w-sm w-full shadow-2xl border border-slate-100 scale-in-center">
            <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-black text-slate-800 flex items-center gap-2">
                    <Layers className="text-emerald-600" /> Unidades
                </h3>
                <button onClick={() => setShowUnitModal(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                    <X size={20} />
                </button>
            </div>
            
            <p className="text-sm text-slate-500 mb-6 font-medium text-center">Você possui múltiplas unidades. Selecione por quais deseja votar agora:</p>
            
            <div className="space-y-2 mb-8 max-h-60 overflow-y-auto pr-2 custom-scrollbar">
              {myUnits.map(unit => (
                <label key={unit} className={`flex items-center gap-3 p-4 rounded-2xl border-2 transition-all cursor-pointer ${tempSelectedUnits.includes(unit) ? 'border-emerald-500 bg-emerald-50' : 'border-slate-100 hover:bg-slate-50'}`}>
                  <input 
                    type="checkbox" 
                    checked={tempSelectedUnits.includes(unit)}
                    onChange={() => {
                        toggleUnit(unit);
                    }}
                    className="w-5 h-5 text-emerald-600 rounded-lg border-slate-300 focus:ring-emerald-500"
                  />
                  <span className="font-bold text-slate-700">{unit}</span>
                </label>
              ))}
            </div>

            <div className="space-y-3">
              <button 
                onClick={() => submitFinalVote(tempSelectedUnits)}
                disabled={tempSelectedUnits.length === 0}
                className="w-full bg-slate-900 text-white font-black py-4 rounded-2xl shadow-xl hover:bg-slate-800 disabled:opacity-30 transition-all flex items-center justify-center gap-2"
              >
                Confirmar {tempSelectedUnits.length} Voto(s)
              </button>
              <button onClick={() => setShowUnitModal(false)} className="w-full text-slate-400 font-bold py-2 hover:text-slate-600 transition-colors text-center">Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default VotingRoom;