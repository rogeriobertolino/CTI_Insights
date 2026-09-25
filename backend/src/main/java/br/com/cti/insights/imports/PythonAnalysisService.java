package br.com.cti.insights.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PythonAnalysisService {
    private final ObjectMapper mapper;
    @Value("${cti.python.executable:python}") private String executable;
    @Value("${cti.python.script:../python/analyze.py}") private String script;
    @Value("${cti.python.timeout-seconds:120}") private long timeoutSeconds;
    public PythonAnalysisService(ObjectMapper mapper){this.mapper=mapper;}

    public Map<String,Object> analyze(Path file) throws Exception {
        Path directory=Files.createTempDirectory("cti-analysis-");
        Path output=directory.resolve("output.json");
        Path error=directory.resolve("error.log");
        Process process=null;
        try {
            ProcessBuilder builder=new ProcessBuilder(executable,script,file.toAbsolutePath().toString());
            builder.environment().put("PYTHONIOENCODING","utf-8");
            // Separate files prevent either pipe filling while the other is being read.
            process=builder.redirectOutput(output.toFile()).redirectError(error.toFile()).start();
            if(!process.waitFor(timeoutSeconds,TimeUnit.SECONDS)) {
                throw new IllegalStateException("O processamento da planilha excedeu o tempo limite.");
            }
            if(process.exitValue()!=0) {
                throw new IllegalStateException("Falha no processamento Python: "+Files.readString(error,StandardCharsets.UTF_8));
            }
            return mapper.readValue(Files.readString(output,StandardCharsets.UTF_8),new TypeReference<>(){});
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if(process!=null && process.isAlive()) {
                process.destroyForcibly();
                // Wait until Windows releases the redirected files before removing them.
                process.onExit().join();
            }
            Files.deleteIfExists(output);
            Files.deleteIfExists(error);
            Files.deleteIfExists(directory);
        }
    }
}
