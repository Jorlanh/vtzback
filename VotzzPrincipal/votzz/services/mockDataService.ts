import { BlogPost } from '../types';

export const MockService = {
  getBlogPosts: async (): Promise<BlogPost[]> => {
    return [
      {
        id: '1',
        title: 'Como realizar uma Assembleia Digital com segurança jurídica',
        slug: 'assembleia-digital-seguranca-juridica',
        content: '<p>A realização de assembleias virtuais tornou-se uma realidade...</p>',
        date: new Date().toISOString(),
        author: 'Dra. Ana Silva',
        category: 'Legislação',
        excerpt: 'Entenda os requisitos legais para validar suas decisões online.',
        imageUrl: 'https://images.unsplash.com/photo-1556761175-5973dc0f32e7?ixlib=rb-1.2.1&auto=format&fit=crop&w=1600&q=80',
        tags: ['Jurídico', 'Assembleia', 'Tecnologia']
      },
      {
        id: '2',
        title: '5 Dicas para reduzir a inadimplência no seu condomínio',
        slug: 'reduzir-inadimplencia-condominio',
        content: '<p>A inadimplência é um dos maiores desafios...</p>',
        date: new Date(Date.now() - 86400000).toISOString(),
        author: 'Carlos Eduardo',
        category: 'Finanças',
        excerpt: 'Estratégias práticas para melhorar o fluxo de caixa.',
        imageUrl: 'https://images.unsplash.com/photo-1554224155-98406858d0cb?ixlib=rb-1.2.1&auto=format&fit=crop&w=1600&q=80',
        tags: ['Finanças', 'Gestão', 'Dicas']
      },
      {
        id: '3',
        title: 'Tecnologia e Segurança: O futuro da portaria remota',
        slug: 'futuro-portaria-remota',
        content: '<p>Sistemas de acesso modernos garantem mais segurança...</p>',
        date: new Date(Date.now() - 172800000).toISOString(),
        author: 'Eng. Roberto Campos',
        category: 'Tecnologia',
        excerpt: 'Como a automação está transformando a segurança patrimonial.',
        imageUrl: 'https://images.unsplash.com/photo-1558008258-3256797b43f3?ixlib=rb-1.2.1&auto=format&fit=crop&w=1600&q=80',
        tags: ['Tecnologia', 'Segurança', 'Inovação']
      }
    ] as any; // Cast to any to avoid "unknown property" errors if types.ts is outdated
  },

  getBlogPostBySlug: async (slug: string): Promise<BlogPost | undefined> => {
    const posts = await MockService.getBlogPosts();
    // @ts-ignore
    return posts.find(p => p.slug === slug);
  }
};