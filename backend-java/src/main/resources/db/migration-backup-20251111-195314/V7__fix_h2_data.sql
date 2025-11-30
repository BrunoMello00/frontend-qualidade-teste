-- Migração V8: Aplicar migração V7 em H2 Database (aplicação local)
-- Corrigir dados existentes e garantir consistência

-- Atualizar produtos sem usuário de cadastro (definir como usuário admin se existir)
UPDATE produtos 
SET usuario_cadastro_id = (
    SELECT id FROM usuarios WHERE email = 'admin@test.com' ORDER BY id LIMIT 1
)
WHERE usuario_cadastro_id IS NULL 
  AND EXISTS (SELECT 1 FROM usuarios WHERE email = 'admin@test.com');

-- Gerar códigos internos sequenciais para produtos existentes (usando ID se for NULL)
UPDATE produtos 
SET codigo_interno_sequencial = id
WHERE codigo_interno_sequencial IS NULL;

-- Garantir que todos os produtos tenham prefixo de código
UPDATE produtos 
SET prefixo_codigo = 'PROD'
WHERE prefixo_codigo IS NULL OR TRIM(prefixo_codigo) = '';

-- Garantir que produtos tenham valores padrão para novos campos
UPDATE produtos 
SET custo_unitario = 0.00
WHERE custo_unitario IS NULL;

-- Garantir que todos os produtos estejam marcados como ativos
UPDATE produtos 
SET ativo = 1
WHERE ativo IS NULL;