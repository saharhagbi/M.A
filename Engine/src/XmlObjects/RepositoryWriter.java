package XmlObjects;

import Objects.Commit;
import Objects.Folder;
import System.Branch;
import System.Engine;
import System.Repository;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static System.Repository.WritingStringInAFile;
import static System.Repository.sf_PathForBranches;
import static XmlObjects.XMLParser.sf_Slash;

public class RepositoryWriter
{
    private final static String sf_AdditionalPathToHEAD = "\\HEAD";
    // private final static String sf_AdditionalPathToMagit = "\\.magit";

    private Repository m_RepositoryToWrite;
    private Map<String, Commit> m_MapSHA1ToCommit;
    private List<Commit> m_CommitsThatHaveBeenWritten;

    public RepositoryWriter(Repository i_RepositoryToWrite, Map<String, Commit> i_AllCommitsIDToCommit)
    {
        this.m_RepositoryToWrite = i_RepositoryToWrite;
        this.m_MapSHA1ToCommit = createMapSHA1ToCommit(i_AllCommitsIDToCommit);
        this.m_CommitsThatHaveBeenWritten = new ArrayList<Commit>();
    }

    public static void CreateDirectory(String i_PathToMakeDir)
    {
        File tempFileForMakingDir = new File(i_PathToMakeDir);

        if (!tempFileForMakingDir.exists())
            tempFileForMakingDir.mkdir();
    }

    public static void WritingFileByPath(String i_PathForWriting, String i_ContentTWrite) throws IOException
    {
        File newFileToWrite = new File(i_PathForWriting);

        Path fixedPathFile = Paths.get(i_PathForWriting);

        if (!newFileToWrite.exists())
            Files.createFile(fixedPathFile);

        FileUtils.writeStringToFile(fixedPathFile.toFile(), i_ContentTWrite, "UTF-8");
    }

    public void WriteRepositoryToFileSystem(String i_NameHeadBranch) throws IOException, ParseException
    {
        CreateDirectory(m_RepositoryToWrite.getRepositoryPath().toString());
        Engine.CreateRepositoryDirectories(Paths.get(m_RepositoryToWrite.getRepositoryPath().toString()));

        for (Branch currentBranch : m_RepositoryToWrite.getAllBranches())
        {
            Commit currentCommit = currentBranch.getCurrentCommit();

            if (!m_CommitsThatHaveBeenWritten.contains(currentCommit))
                writeCommitAndAllPrevCommits(currentCommit);

            String pathForWritingBranch = m_RepositoryToWrite.getRepositoryPath().toString() + sf_PathForBranches;


            checkIfCurrentBranchIsHeadAndUpdateIfItDoes(currentBranch.getBranchName(), i_NameHeadBranch,
                    pathForWritingBranch);

            WritingFileByPath(pathForWritingBranch + sf_Slash + currentBranch.getBranchName()
                    + Repository.sf_txtExtension, currentCommit.getSHA1());
        }
        Folder.SpanDirectory(m_RepositoryToWrite.getActiveBranch().getCurrentCommit().getRootFolder());
    }

    private void checkIfCurrentBranchIsHeadAndUpdateIfItDoes(String i_CurrentBranchName, String i_NameHeadBranch,
                                                             String i_PathForWritingBranch) throws IOException
    {
        if (i_NameHeadBranch.equals(i_CurrentBranchName))
            WritingFileByPath(i_PathForWritingBranch + sf_AdditionalPathToHEAD
                    + Repository.sf_txtExtension, i_NameHeadBranch);
    }

    private void writeCommitAndAllPrevCommits(Commit i_CurrentCommit) throws ParseException, IOException
    {
        String SHA1OfPrevCommit = i_CurrentCommit.getPrevCommitSha1();
        Commit prevCommit = m_MapSHA1ToCommit.get(SHA1OfPrevCommit);

        if ((!m_CommitsThatHaveBeenWritten.contains(i_CurrentCommit)) && prevCommit != null)
            writeCommitAndAllPrevCommits(prevCommit);

        putCommitInObjectsFile(i_CurrentCommit);

        m_CommitsThatHaveBeenWritten.add(i_CurrentCommit);
    }

    private void putCommitInObjectsFile(Commit i_CurrentCommit) throws ParseException, IOException
    {
        m_RepositoryToWrite.commitAllObjectsInAFolder(i_CurrentCommit.getRootFolder(), i_CurrentCommit.getUserCreated());

        Path pathForWritingWC = i_CurrentCommit.getRootFolder().WritingFolderAsATextFile();
        m_RepositoryToWrite.zipAndPutInObjectsFolder(pathForWritingWC.toFile(),
                i_CurrentCommit.getRootFolder().getSHA1());

        //putting Commit in objects
        String contentOfCommit = i_CurrentCommit.CreatingContentOfCommit();
        Path pathForWritingCommit = WritingStringInAFile(contentOfCommit, i_CurrentCommit.getSHA1());
        m_RepositoryToWrite.zipAndPutInObjectsFolder(pathForWritingCommit.toFile(), i_CurrentCommit.getSHA1());
    }

    private Map<String, Commit> createMapSHA1ToCommit(Map<String, Commit> i_allCommitsIDToCommit)
    {
        Map<String, Commit> newMapSHA1ToCommit = new HashMap<String, Commit>();

        for (Map.Entry<String, Commit> currentItemCommit : i_allCommitsIDToCommit.entrySet())
            newMapSHA1ToCommit.put(currentItemCommit.getValue().getSHA1(), currentItemCommit.getValue());

        return newMapSHA1ToCommit;
    }
}
