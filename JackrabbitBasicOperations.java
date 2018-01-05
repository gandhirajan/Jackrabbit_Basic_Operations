import org.apache.jackrabbit.rmi.repository.URLRemoteRepository;

import javax.jcr.*;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import static javax.jcr.nodetype.NodeType.*;

public class JackrabbitBasicOperations {

    public static void main(String[] args) throws Exception{
        JackrabbitBasicOperations main = new JackrabbitBasicOperations();
        if("add".equalsIgnoreCase(args[0])) {
            addDocument();
        }
        else if("update".equalsIgnoreCase(args[0])) {
            updateDocument();

        }
        else if("delete".equalsIgnoreCase(args[0])) {
            deleteDocument();
        }
        else if("version".equalsIgnoreCase(args[0])) {
            getVersionInfo();
        }
        else if("delversion".equalsIgnoreCase(args[0])) {
            delVersionInfo(args[1]);
        }
        else if("copy".equalsIgnoreCase(args[0])) {
            copyDocument();
        }
    }

    private static void addDocument() throws MalformedURLException, RepositoryException, FileNotFoundException {
        JackrabbitBasicOperations main = new JackrabbitBasicOperations();
        Session session  = main.getJackrabbitSession();
        Node rootNode = session.getRootNode();
        Node appFileNode  = main.createFolders("JACKRABBIT",session,rootNode);
        Node destNode = main.createFolders("Month\\Date",session,appFileNode);
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        versionManager.checkout(destNode.getPath());
        Node fileNode = null;
        try {
            fileNode = destNode.addNode("Test", "nt:file");
        }
        catch (ItemExistsException e) {
            fileNode = main.createFolders("Test",session,destNode);
            System.out.println("Doc_Id - " + fileNode.getIdentifier());
            System.out.println("File already exists !!!");
            return;
        }
        fileNode.addMixin(MIX_VERSIONABLE);
        fileNode.addMixin(MIX_LOCKABLE);
        fileNode.addMixin(MIX_REFERENCEABLE);
        Node resNode = fileNode.addNode(DMSConstants.RESOURCE_NODE,"nt:unstructured");
        if (!resNode.isCheckedOut()) {
            versionManager.checkout(resNode.getPath());
        }
        fileNode.addMixin(DMSConstants.MIX_TITLE);
        resNode = main.setValueToNode(resNode);
        ValueFactory valueFactory = session.getValueFactory();
        File file = new File("TestDoc.txt");
        resNode.setProperty(DMSConstants.DATA_PROPERTY, valueFactory.createBinary(new FileInputStream(file)));
        session.save();
        versionManager.checkin(resNode.getPath());
        System.out.println("File added successfully !!!");
    }

    public static void updateDocument() throws Exception {
        JackrabbitBasicOperations main = new JackrabbitBasicOperations();
        Session session  = main.getJackrabbitSession();
        Node destNode = session.getNodeByIdentifier("<node_id_of_existing_doc_in_repo>");
        if(destNode == null) {
            System.out.println("File not found");
        }
        Node resNode = destNode.getNode(DMSConstants.RESOURCE_NODE);
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        if (!resNode.isCheckedOut()) {
            versionManager.checkout(resNode.getPath());
        }

        destNode.addMixin(DMSConstants.MIX_TITLE);
        resNode = main.setValueToNode(resNode);
        ValueFactory valueFactory = session.getValueFactory();
        File file = new File("TestDoc_Update.txt");
        resNode.setProperty("jcr:data", valueFactory.createBinary(new FileInputStream(file)));
        session.save();
        versionManager.checkin(resNode.getPath());
        System.out.println("File updated successfully !!!");
    }

    public static void copyDocument() throws Exception {
        JackrabbitBasicOperations main = new JackrabbitBasicOperations();
        Session session  = main.getJackrabbitSession();
        Node srcNode = session.getNodeByIdentifier("<node_id_of_existing_doc_in_repo>");
        if(srcNode == null) {
            System.out.println("File not found");
        }

        Node rootNode = session.getRootNode();
        Node appFileNode  = main.createFolders("JACKRABBIT",session,rootNode);
        String destNodeName = "Copy_Test_"+System.currentTimeMillis();
        System.out.println("destNodeName - " + destNodeName);
        session.getWorkspace().copy(srcNode.getPath(),appFileNode.getPath() + "/" + destNodeName);
        Node destNode = appFileNode.getNode(destNodeName);
        destNode.addMixin(MIX_VERSIONABLE);
        destNode.addMixin(MIX_LOCKABLE);
        destNode.addMixin(MIX_REFERENCEABLE);
        session.save();
        System.out.println("id - " + destNode.getIdentifier());
    }

    public static void deleteDocument() throws Exception {
        JackrabbitBasicOperations main = new JackrabbitBasicOperations();
        Session session  = main.getJackrabbitSession();

        Node destNode = session.getNodeByIdentifier("<node_id_of_existing_doc_in_repo>");
        if (destNode != null && !destNode.getPrimaryNodeType().getName().equals("jcr:system")) {
            System.out.println("Name - " + destNode.getName());
            destNode.remove();
        }
        session.save();
        System.out.println("File deleted successfully !!!");
    }

    public static void getVersionInfo() throws Exception {
        JackrabbitBasicOperations main = new JackrabbitBasicOperations();
        Session session  = main.getJackrabbitSession();
        Node destNode = session.getNodeByIdentifier("<node_id_of_existing_doc_in_repo>");
        if(destNode == null) {
            System.out.println("File not found");
        }
        Node resNode = destNode.getNode(DMSConstants.RESOURCE_NODE);
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        VersionHistory vh = versionManager.getVersionHistory(resNode.getPath());
        VersionIterator vi = vh.getAllVersions();
        vi.skip(1);
        int version = 1;
        while (vi.hasNext()) {
            Version v = vi.nextVersion();
            System.out.println(v.getName());
            NodeIterator ni = v.getNodes();
            while (ni.hasNext()) {
                Node nv = ni.nextNode();
                InputStream stream = nv.getProperty(DMSConstants.DATA_PROPERTY).getValue().getBinary().getStream();
                File targetFile = new File("version" + version + ".txt");
                Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            version ++;
        }
        session.save();
        System.out.println("File versioned successfully !!!");
    }

    public static void delVersionInfo(String versionNo) throws Exception {
        JackrabbitBasicOperations main = new JackrabbitBasicOperations();
        Session session  = main.getJackrabbitSession();
        Node destNode = session.getNodeByIdentifier("<node_id_of_existing_doc_in_repo>");
        if(destNode == null) {
            System.out.println("File not found");
        }
        Node resNode = destNode.getNode(DMSConstants.RESOURCE_NODE);
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        VersionHistory vh = versionManager.getVersionHistory(resNode.getPath());
        vh.removeVersion(versionNo);
        session.save();
        System.out.println("File version deleted successfully !!!");
        getVersionInfo();
    }

    public Session getJackrabbitSession() throws MalformedURLException {
        Session session =null;

        try{
            String remoteRepositoryName = "http://<hostname>:<port>/<jackrabbit_context>/rmi";
            String userName = "admin";
            String password = "admin";
            Repository repository = new URLRemoteRepository(remoteRepositoryName);
            session = repository.login(new SimpleCredentials(userName, password.toCharArray()));
        }catch(RepositoryException e){
            e.printStackTrace();
        }
        return session;
    }

    private Node createFolders(String folderPath, Session session, Node rootNode) {
        Node folderNode = null;
        try {
            folderNode = rootNode;
            if (folderPath != null && folderPath.length() > 1) {
                StringTokenizer stringTokenizer = new StringTokenizer(folderPath, "\\");
                while (stringTokenizer.hasMoreTokens()) {
                    String token = stringTokenizer.nextToken();
                    Node subFolderNode = null;
                    try {
                        subFolderNode = folderNode.getNode(token);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    if (subFolderNode != null) {
                        folderNode = subFolderNode;
                    } else {
                        folderNode = folderNode.addNode(token, "nt:folder");
                        folderNode.addMixin(MIX_VERSIONABLE);
                        folderNode.addMixin(MIX_LOCKABLE);
                        folderNode.addMixin(MIX_REFERENCEABLE);
                        session.save();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return folderNode;
    }
    private Node setValueToNode(Node resNode) throws RepositoryException {
        resNode.setProperty(DMSConstants.MIME_PROPERTY, "text/plain");
        resNode.setProperty(DMSConstants.ENCODING_PROPERTY, "UTF-8");
        Calendar currentCalendarDate = Calendar.getInstance();
        currentCalendarDate.setTime(new Date());
        resNode.setProperty(DMSConstants.JCR_LAST_MODIFIED, currentCalendarDate);
        resNode.setProperty(DMSConstants.AUTHOR_PROPERTY, "G");
        resNode.setProperty(DMSConstants.DOC_NAME, "Test.txt");
        resNode.addMixin(MIX_VERSIONABLE);
        resNode.addMixin(MIX_LOCKABLE);
        resNode.addMixin(MIX_REFERENCEABLE);
        return resNode;
    }

}
