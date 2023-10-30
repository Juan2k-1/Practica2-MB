package juan.motores_busqueda.practica2_mb;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class ParserQueries
{

    private static String extractWordsFromLine(BufferedReader reader, StringBuilder queryTextBuilder) throws IOException
    {
        Pattern wordPattern = Pattern.compile("\\w+");

        String line;
        while ((line = reader.readLine()) != null && !line.startsWith("."))
        {
            Matcher matcher = wordPattern.matcher(line);
            while (matcher.find() && queryTextBuilder.length() < 5)
            {
                queryTextBuilder.append(matcher.group()).append(" ");
            }
        }
        return queryTextBuilder.toString().trim();
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args)
    {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Por favor, proporciona la ruta del archivo CISI.QRY como argumento.");
        String filePath = scanner.nextLine();
        Path pathToDocument = Paths.get(filePath);

        // Conexión al servidor Solr
        String solrUrl = "http://localhost:8983/solr/CORPUS2";
        HttpSolrClient solrClient = new HttpSolrClient.Builder(solrUrl).build();

        try ( BufferedReader br = Files.newBufferedReader(pathToDocument.toAbsolutePath()))
        {
            String line;
            StringBuilder queryTextBuilder = new StringBuilder();

            while ((line = br.readLine()) != null)
            {
                if (line.startsWith(".I"))
                {
                    // No necesitamos la ID 
                } else if (line.startsWith(".W"))
                {
                    String queryText = extractWordsFromLine(br, queryTextBuilder);

                    // Construir la consulta a Solr
                    SolrQuery solrQuery = new SolrQuery();
                    solrQuery.setQuery("content:" + queryText);
                    solrQuery.set("fl", "id,title,score");

                    // Realizar la consulta a Solr y procesar los resultados
                    QueryResponse response = solrClient.query(solrQuery);

                    // Procesar los resultados de Solr
                    SolrDocumentList docs = response.getResults();
                    for (int i = 0; i < docs.size(); ++i)
                    {
                        SolrDocument doc = docs.get(i);
                        String id = (String) doc.getFieldValue("id");
                        String title = (String) doc.getFieldValue("title");
                        Float score = (Float) doc.getFieldValue("score");

                        System.out.println("ID: " + id);
                        System.out.println("Title: " + title);
                        System.out.println("Score: " + score); // Mostrar el score
                        System.out.println("--------------------");

                        /*System.out.println(docs.get(i));
                        System.out.println("--------------------");*/
                    }
                    // Limpiar el StringBuilder para la próxima consulta
                    queryTextBuilder.setLength(0);
                }
            }
            solrClient.close();
            scanner.close();
        } catch (IOException | SolrServerException e)
        {
            Logger.getLogger(ParserQueries.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }
}