package XmlObjects;


import Objects.Blob;
import Objects.Commit;
import Objects.Folder;
import Objects.Item;
import System.Branch;
import System.Repository;
import System.User;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class XMLParser
{
    public static final String sf_Slash = "\\";
    private Commit m_CurrentCommit;
    private Branch m_ActiveBranch;
    private MagitRepository m_MagitRepository;
    private MagitBranches m_MagitBranches;
    private MagitCommits m_MagitCommits;
    private MagitFolders m_MagitFolders;
    private MagitBlobs m_MagitBlobs;

    private Path m_PathToBranchesDir;
    private Path m_PathToObjectsDir;

    //There are always Commits!
    //private Map<String, String> m_AllCommitsIDToSHA1 = new HashMap<String, String>();

    private Map<String, Commit> m_AllCommitsIDToCommit;

    public void setAllObjects(MagitRepository i_MagitRepository)
    {
        this.m_MagitRepository = i_MagitRepository;
        m_MagitBranches = i_MagitRepository.magitBranches;
        m_MagitCommits = i_MagitRepository.magitCommits;
        m_MagitFolders = i_MagitRepository.magitFolders;
        m_MagitBlobs = i_MagitRepository.magitBlobs;
        m_AllCommitsIDToCommit = new HashMap<String, Commit>();

        m_PathToBranchesDir = Paths.get(i_MagitRepository.location + Repository.sf_PathForBranches);
        m_PathToObjectsDir = Paths.get(i_MagitRepository.location + Repository.sf_PathForObjects);
    }

    public Repository parseRepositoryFromXmlFile() throws Exception
    {
        Repository repoToCreate;

        List<MagitSingleBranch> listOfMSB = m_MagitBranches.getMagitSingleBranch();
        List<Branch> listRepositoryBranches = new ArrayList<Branch>();

        for (MagitSingleBranch magitSingleBranch : listOfMSB)
        {
            MagitSingleCommit currentPointedMSC = findComitByID(magitSingleBranch.pointedCommit.id);

            Commit currentCommit;
            if (!isIDOfMSCExistInMap(currentPointedMSC))
                currentCommit = createCurrentCommitAndItsAllPrevCommits(currentPointedMSC);
            else
                currentCommit = m_AllCommitsIDToCommit.get(currentPointedMSC.id);

            Branch currentBranch = new Branch(magitSingleBranch.name, currentCommit);

            checkIfCurrentBranchIsHEAD(magitSingleBranch, currentBranch);

            listRepositoryBranches.add(currentBranch);
        }

        repoToCreate = new Repository(m_ActiveBranch, Paths.get(m_MagitRepository.location),
                m_MagitRepository.name, listRepositoryBranches);

        RepositoryWriter writer = new RepositoryWriter(repoToCreate, m_AllCommitsIDToCommit);
        writer.WriteRepositoryToFileSystem(m_ActiveBranch.getBranchName());

        return repoToCreate;
    }

    private void checkIfCurrentBranchIsHEAD(MagitSingleBranch magitSingleBranch, Branch currentBranch)
    {
        if (magitSingleBranch.name.equals(m_MagitBranches.head))
        {
            m_ActiveBranch = currentBranch;
        }
    }

    private Commit createCurrentCommitAndItsAllPrevCommits(MagitSingleCommit i_CurrentPointedMSC) throws ParseException
    {
        String precedingCommitID = getPrecedingCommitID(i_CurrentPointedMSC);

        if ((precedingCommitID != null) && (!isIDOfMSCExistInMap(i_CurrentPointedMSC)))
            createCurrentCommitAndItsAllPrevCommits(findComitByID(precedingCommitID));

        Commit currentCommit = createCommit(i_CurrentPointedMSC);

        m_AllCommitsIDToCommit.put(i_CurrentPointedMSC.id, currentCommit);

        return currentCommit;
    }

    private Commit createCommit(MagitSingleCommit i_CurrentPointedMSC) throws ParseException
    {
        User userCreated;
        Date dateOfCreation;
        String SHA1OfPrevCommit = null;
        Commit prevCommit;
        Folder folderToCreate;

        MagitSingleFolder rootMSFOfCommit = findFolderByID(i_CurrentPointedMSC.rootFolder.id);

        userCreated = new User(i_CurrentPointedMSC.author);

        //working except of name in rootFolder
        folderToCreate = createFolder(rootMSFOfCommit, m_MagitRepository.location);

        if (thereIsPrecedingCommit(i_CurrentPointedMSC))
        {
            prevCommit = m_AllCommitsIDToCommit.get(getPrecedingCommitID(i_CurrentPointedMSC));
            SHA1OfPrevCommit = prevCommit.getSHA1();
        }

        dateOfCreation = XMLDateFormatter.FormatStringToDateType(i_CurrentPointedMSC.dateOfCreation);
        String CommitSha1;
        CommitSha1 = Commit.createSha1ForCommit(folderToCreate, SHA1OfPrevCommit, i_CurrentPointedMSC.message,
                userCreated, dateOfCreation);
        Commit commitToReturn = new Commit(CommitSha1,
                folderToCreate,
                SHA1OfPrevCommit,
                i_CurrentPointedMSC.message,
                userCreated, dateOfCreation);

        return commitToReturn;
    }

    private Folder createFolder(MagitSingleFolder i_RootMSFOfCommit, String i_CurrentLocation) throws ParseException
    {
        List<Item> allItemInCurrentFolder = new ArrayList<Item>();

        for (XmlObjects.Item item : i_RootMSFOfCommit.items.getItem())
        {
            if (item.type.equals(XMLValidate.BLOB_TYPE))
            {
                createBlobObjectToSystem(i_CurrentLocation, allItemInCurrentFolder, item);
            } else//is folder
            {
                MagitSingleFolder currentMSF = findFolderByID(item.id);
                Folder folderReturnedFromRecursion = createFolder(currentMSF,
                        i_CurrentLocation + sf_Slash + currentMSF.name);

                allItemInCurrentFolder.add(folderReturnedFromRecursion);
            }
        }

        Folder currentFolder = new Folder(allItemInCurrentFolder, i_CurrentLocation,
                i_RootMSFOfCommit.name, Folder.CreateSHA1ForFolderFile(allItemInCurrentFolder), Item.TypeOfFile.FOLDER,
                new User(i_RootMSFOfCommit.lastUpdater),
                XMLDateFormatter.FormatStringToDateType(i_RootMSFOfCommit.lastUpdateDate));

        return currentFolder;
    }

    private void createBlobObjectToSystem(String i_CurrentLocation, List<Item> i_AllItemInCurrentFolder, XmlObjects.Item i_Item) throws ParseException
    {
        MagitBlob currentMSB = findBlobByID(i_Item.id);
        Path pathForBlob = Paths.get(i_CurrentLocation + sf_Slash + currentMSB.name);
        String SHA1ForBlob = DigestUtils.sha1Hex(currentMSB.content);

        Blob blobToCreate = new Blob(pathForBlob, SHA1ForBlob,
                currentMSB.content, Item.TypeOfFile.BLOB, new User(currentMSB.lastUpdater),
                XMLDateFormatter.FormatStringToDateType(currentMSB.lastUpdateDate),
                currentMSB.name);

        i_AllItemInCurrentFolder.add(blobToCreate);
    }

    private String getPrecedingCommitID(MagitSingleCommit i_CurrentPointedMSC)
    {
        if ((i_CurrentPointedMSC.precedingCommits == null) ||
                (i_CurrentPointedMSC.precedingCommits.precedingCommit == null))
            return null;

        return i_CurrentPointedMSC.precedingCommits.precedingCommit.get(0).id;

        /*int amountOfPrecedingCommit = i_CurrentPointedMSC.precedingCommits.getPrecedingCommit().size();
        return i_CurrentPointedMSC.precedingCommits.getPrecedingCommit().get(amountOfPrecedingCommit - 1).id;*/
    }

    private MagitSingleCommit findComitByID(String i_Id)
    {
        MagitSingleCommit magitSingleCommitToFind = null;

        for (MagitSingleCommit magitSingleCommit : m_MagitCommits.getMagitSingleCommit())
        {
            if (magitSingleCommit.id.equals(i_Id))
                magitSingleCommitToFind = magitSingleCommit;
        }

        return magitSingleCommitToFind;
    }

    private MagitSingleFolder findFolderByID(String i_Id)
    {
        MagitSingleFolder magitSingleFolderToFind = null;

        for (MagitSingleFolder magitSingleFolder : m_MagitFolders.getMagitSingleFolder())
        {
            if (magitSingleFolder.id.equals(i_Id))
                magitSingleFolderToFind = magitSingleFolder;
        }

        return magitSingleFolderToFind;
    }

    private MagitBlob findBlobByID(String i_Id)
    {
        MagitBlob magitBlobToFind = null;

        for (MagitBlob magitBlob : m_MagitBlobs.getMagitBlob())
        {
            if (magitBlob.id.equals(i_Id))
                magitBlobToFind = magitBlob;
        }

        return magitBlobToFind;
    }

    private boolean isIDOfMSCExistInMap(MagitSingleCommit currentPointedMSC)
    {
        return m_AllCommitsIDToCommit.containsKey(currentPointedMSC.id);
    }

    private boolean thereIsPrecedingCommit(MagitSingleCommit i_currentPointedMSC)
    {
        if (i_currentPointedMSC.precedingCommits == null)
            return false;

        return i_currentPointedMSC.precedingCommits.precedingCommit != null;
    }
}