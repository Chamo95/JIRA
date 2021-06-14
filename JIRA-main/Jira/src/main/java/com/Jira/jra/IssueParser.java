package com.Botronsoft.jra;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.codehaus.jackson.map.ObjectMapper;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;


public class IssueParser {

	public static void main(String[] args) {

		System.out.println("Choose which format to persist the data : XML or JSON");
		Scanner s = new Scanner(System.in);
		String format = s.nextLine().toUpperCase();
		
		JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
		URI uri;
		JiraRestClient client = null;
		try {
			uri = new URI("https://jira.atlassian.com");
			client = factory.create(uri, new AnonymousAuthenticationHandler());

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		Promise<SearchResult> searchJqlPromise = client.getSearchClient()
				.searchJql("issuetype in (Bug, Documentation, Enhancement) and updated > startOfWeek()");

		if (format.equals("JSON")) {
			s.close();
			try {
				parseToJSON(collectedIssues(searchJqlPromise));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if (format.equals("XML")) {
			
			try {
				parseToXML(collectedIssues(searchJqlPromise));
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JAXBException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Wrong input:restart the program");
		}

	}

	public static List<IssueEntity> collectedIssues(Promise<SearchResult> searchJqlPromise) {
		List<IssueEntity> issueEntities = new ArrayList<IssueEntity>();
		for (Issue issue : searchJqlPromise.claim().getIssues()) {
			IssueEntity issueEntity = new IssueEntity();
			issueEntity.setSummary(issue.getSummary());
			issueEntity.setKey(issue.getKey());
			issueEntity.setUri(issue.getSelf());
			issueEntity.setType(issue.getIssueType().getName());
			issueEntity.setPriority(issue.getPriority().getName());
			issueEntity.setDescription(issue.getDescription());
			issueEntity.setReporterName(issue.getReporter().getDisplayName());
			Date date = issue.getCreationDate().toDate();
			String sth = new SimpleDateFormat("yyyy-MM-dd").format(date);
			issueEntity.setCreationDate(sth);
			List<ProcessedComment> comments = new ArrayList<ProcessedComment>();
			for (Comment comment : issue.getComments()) {
				
				comments.add(new ProcessedComment(comment.getBody(), comment.getAuthor().getDisplayName()));
			}
			
			issueEntity.setComments(comments);
			issueEntities.add(issueEntity);
		}
		
		return issueEntities;
		
	}

	public static void parseToJSON(List<IssueEntity> issueEntities) throws IOException {
	
		ObjectMapper om=new ObjectMapper();
		om.writeValue(new File("issues.json"),issueEntities);
		System.out.println("Persisting data to file : issue.json is completed");
	}

	public static void parseToXML(List<IssueEntity> issueEntities) throws IOException, JAXBException {
		Writer XMLWriter = new FileWriter("issues.xml");
		JAXBContext jaxbContext = JAXBContext.newInstance(IssueEntity.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		for (IssueEntity issueEntity : issueEntities) {
			jaxbMarshaller.marshal(issueEntity, XMLWriter);
			XMLWriter.write("\n");
			jaxbMarshaller.setProperty("com.sun.xml.bind.xmlDeclaration", Boolean.FALSE);
		}
		
		XMLWriter.close();
		System.out.println("Persisting data to file : issue.xml is completed");
	}

}