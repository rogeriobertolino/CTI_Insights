package br.com.cti.insights.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

@Service
public class PythonAnalysisService {
    private final ObjectMapper mapper;
    @Value("${cti.python.executable:python}") private String executable;
    @Value("${cti.python.script:../python/analyze.py}") private String script;
    public PythonAnalysisService(ObjectMapper mapper){this.mapper=mapper;}

    public Map<String,Object> analyze(Path file) throws Exception {
        Process process=new ProcessBuilder(executable,script,file.toAbsolutePath().toString()).redirectErrorStream(false).start();
        String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        String error=new String(process.getErrorStream().readAllBytes(),StandardCharsets.UTF_8);
        if(process.waitFor()!=0) throw new IllegalStateException("Falha no processamento Python: "+error);
        return mapper.readValue(output,new TypeReference<>(){});
    }
}
