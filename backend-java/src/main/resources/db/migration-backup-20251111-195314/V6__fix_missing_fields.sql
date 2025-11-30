-- Migração V7: Corrigir campos faltantes e valores padrão
-- Atualizar produtos existentes que não têm usuário de cadastro nem código interno sequencial

-- Atualizar produtos sem usuário de cadastro (definir como usuário admin)
UPDATE produtos 
SET usuario_cadastro_id = (SELECT id FROM usuarios WHERE email = 'admin@test.com' LIMIT 1)
WHERE usuario_cadastro_id IS NULL;

-- Gerar códigos internos sequenciais para produtos existentes
UPDATE produtos 
SET codigo_interno_sequencial = id
WHERE codigo_interno_sequencial IS NULL;

-- Garantir que todos os produtos tenham prefixo de código
UPDATE produtos 
SET prefixo_codigo = 'PROD'
WHERE prefixo_codigo IS NULL OR prefixo_codigo = '';

-- Comentários de documentação
COMMENT ON COLUMN produtos.usuario_cadastro_id IS 'ID do usuário que cadastrou o produto';
COMMENT ON COLUMN produtos.codigo_interno_sequencial IS 'Código sequencial interno único do produto';
COMMENT ON COLUMN produtos.prefixo_codigo IS 'Prefixo utilizado na geração do código de barras';
COMMENT ON COLUMN produtos.margem IS 'Margem de lucro percentual do produto';