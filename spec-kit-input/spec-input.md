Hoje, só consigo registrar contas do tipo SAÍDA para fornecedores e ENTRADA para unidades. Quero eliminar essa restrição. Agora, quero poder fazer pagamentos a unidades e registrar recebimentos de fornecedores.

Além disso, quero uma linha ao final da tabela de contas que seja a somatória total dos dados na coluna VALOR. Parecido com o que você fez na tabela de Fundos, porém essa somatória deve ser dinâmica: se eu filtro, apago ou adiciono linhas, essa somatória deve atualizar.

Com isso, o filtro Unidade agora se torna Unidade ou Fornecedor. Favor também renomear a coluna Contraparte das contas para Unidade ou Fornecedor.

Além disso, Unidade também deve ter o atributo chave pix, e fornecedor deixa de ter o atributo unidade.

Também quero um filtro para Fundo na página de Contas.

Nessa configuração nova, repare que Fornecedor e Unidade têm exatamente os mesmos campos. Assim, não faz mais sentido duas entidades separadas. Proponha um nome para essa entidade que unificará essas duas.

Mas eu ainda preciso poder adicionar lançamentos a todas as unidades de uma vez, que não quero lançar para os fornecedores. Então pensei no seguinte: e se criarmos o conceito de Grupos? Eu posso adicionar uma entidade (unidade ou fornecedor, não sei que nome ela terá ainda, vamos chamar de X por enquanto) a um grupo. Depois, ao lançar uma conta, eu posso selecionar para lançar para uma X específica ou para um grupo de X selecionado de uma lista suspensa. O que você acha?