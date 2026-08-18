import React, { useState, useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import { askSupportChatbot } from '../services/geminiService';
import { useAuth } from '../context/AuthContext';

interface Message {
  role: 'user' | 'bot';
  content: string;
}

export const SupportChatbot: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const chatEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  // AJUSTE DE SCROLL: Só desce automaticamente quando o USUÁRIO envia mensagem
  useEffect(() => {
    if (messages.length > 0) {
      const lastMessage = messages[messages.length - 1];
      if (lastMessage.role === 'user') {
        scrollToBottom();
      }
    }
  }, [messages]);

  useEffect(() => {
    if (isOpen && messages.length === 0) {
      const welcomeMsg = isAuthenticated
        ? "Olá! Sou a **S.I.R.I.U.S.** **(Sistema Integrado de Respostas e Interação para Usuários e Síndicos)** seu assistente Votzz. Como posso ajudar você a configurar suas assembleias ou gerenciar seu condomínio hoje?"
        : "Olá! Sou a **S.I.R.I.U.S.** **(Sistema Integrado de Respostas e Interação para Usuários e Síndicos)** seu assistente Votzz. Bem-vindo à Votzz. Quer saber como podemos modernizar a gestão do seu condomínio?";
      setMessages([{ role: 'bot', content: welcomeMsg }]);
    }
  }, [isOpen, isAuthenticated, messages.length]);

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;

    const userText = input;
    setInput('');
    setMessages((prev) => [...prev, { role: 'user', content: userText }]);
    setIsLoading(true);

    try {
      const botResponse = await askSupportChatbot(userText, isAuthenticated);
      setMessages((prev) => [...prev, { role: 'bot', content: botResponse }]);
    } catch (error) {
      setMessages((prev) => [...prev, { role: 'bot', content: "Ops! Tive um problema técnico. Pode repetir?" }]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed bottom-5 right-5 z-50 font-sans">
      {!isOpen ? (
        <button 
          onClick={() => setIsOpen(true)}
          className="bg-emerald-600 text-white p-4 rounded-full shadow-2xl hover:scale-110 active:scale-95 transition-all flex items-center justify-center border-2 border-white animate-bounce"
        >
          <span className="text-2xl">💬</span>
        </button>
      ) : (
        <div className="bg-white rounded-2xl shadow-2xl w-[350px] h-[500px] flex flex-col border border-gray-200 overflow-hidden">
          {/* Header */}
          <div className="bg-emerald-600 text-white p-4 flex justify-between items-center shadow-md">
            <div className="flex flex-col">
                <span className="font-black tracking-tighter text-lg leading-none">S.I.R.I.U.S.</span>
                <span className="text-[10px] uppercase opacity-80 font-bold mt-1">Inteligência Artificial Votzz</span>
            </div>
            <button onClick={() => setIsOpen(false)} className="hover:rotate-90 transition-transform duration-300 p-1">
              ✖
            </button>
          </div>
          
          {/* Chat Body */}
          <div className="flex-1 p-4 overflow-y-auto bg-slate-50 flex flex-col gap-4">
            {messages.map((msg, index) => (
              <div 
                key={index} 
                className={`p-3 rounded-2xl max-w-[90%] text-sm shadow-sm ${
                  msg.role === 'user' 
                    ? 'bg-emerald-600 text-white self-end rounded-br-none' 
                    : 'bg-white border border-gray-100 text-slate-700 self-start rounded-bl-none'
                }`}
              >
                {/* RENDERIZADOR MARKDOWN COM ESTILIZAÇÃO PARA NEGRITOS E ESPAÇAMENTO */}
                <div className="prose prose-sm max-w-none break-words leading-relaxed">
                  <ReactMarkdown 
                    components={{
                      // Força o negrito a ser extra-bold e destaca a cor se necessário
                      strong: ({node, ...props}) => <strong className="font-black text-inherit" {...props} />,
                      p: ({node, ...props}) => <p className="mb-2 last:mb-0 leading-normal" {...props} />,
                      ul: ({node, ...props}) => <ul className="mb-2 ml-4 list-disc" {...props} />,
                      li: ({node, ...props}) => <li className="mb-1" {...props} />
                    }}
                  >
                    {msg.content}
                  </ReactMarkdown>
                </div>
              </div>
            ))}
            {isLoading && (
              <div className="flex items-center gap-2 self-start bg-white p-2 rounded-lg border border-gray-100 shadow-sm">
                <div className="w-2 h-2 bg-emerald-500 rounded-full animate-bounce"></div>
                <div className="w-2 h-2 bg-emerald-500 rounded-full animate-bounce [animation-delay:-0.3s]"></div>
                <div className="w-2 h-2 bg-emerald-500 rounded-full animate-bounce [animation-delay:-0.5s]"></div>
              </div>
            )}
            <div ref={chatEndRef} />
          </div>

          {/* Input Area */}
          <div className="p-4 border-t border-slate-100 bg-white">
            <div className="flex gap-2 bg-slate-100 p-2 rounded-xl focus-within:ring-2 focus-within:ring-emerald-500 transition-all">
                <input
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSend()}
                  placeholder="Como posso ajudar?"
                  className="flex-1 bg-transparent border-none text-sm focus:outline-none px-2"
                />
                <button 
                  onClick={handleSend}
                  disabled={isLoading}
                  className="bg-emerald-600 text-white w-8 h-8 rounded-lg flex items-center justify-center hover:bg-emerald-700 disabled:opacity-50 transition-all shadow-md"
                >
                  ➤
                </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};