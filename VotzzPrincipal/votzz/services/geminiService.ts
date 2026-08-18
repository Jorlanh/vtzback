// votzz/services/geminiService.ts
import api from './api';

export const generateAssemblyDescription = async (topic: string, details: string): Promise<string> => {
  try {
    // Sincronizado com o novo padrão do Controller
    const response = await api.post('/ai/generate-description', { topic, details });
    return response.data.response;
  } catch (error) {
    console.error("Erro no backend Gemini:", error);
    return "Não foi possível gerar a descrição com IA no momento.";
  }
};

export const generateNotificationDraft = async (assemblyTitle: string, endDate: string): Promise<string> => {
  try {
    const response = await api.post('/ai/generate-notification', { assemblyTitle, endDate });
    return response.data.response;
  } catch (error) {
    console.error("Erro no backend Gemini:", error);
    return "Erro ao gerar notificação.";
  }
};

export const analyzeSentiment = async (messages: string[]): Promise<string> => {
  if (messages.length === 0) return "Sem dados para análise.";
  try {
    const response = await api.post('/ai/analyze-sentiment', { messages });
    return response.data.response;
  } catch (error) {
    console.error("Erro no backend Gemini:", error);
    return "Não foi possível analisar o chat.";
  }
};

export const askSupportChatbot = async (userMessage: string, isLoggedIn: boolean): Promise<string> => {
  try {
    // Rota unificada para /api/ai/ask
    const response = await api.post('/ai/ask', { 
      message: userMessage, 
      isLoggedIn: isLoggedIn // Nome do campo corrigido para o Java
    });
    return response.data.response;
  } catch (error) {
    console.error("Erro ao chamar o chatbot backend:", error);
    return "Desculpe, meus sistemas estão temporariamente indisponíveis. Tente novamente em instantes.";
  }
};