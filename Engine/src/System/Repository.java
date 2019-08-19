package System;

import Objects.Blob;
import Objects.Commit;
import Objects.Folder;
import Objects.Item;
import XmlObjects.RepositoryWriter;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Repository
{
    //At first, we dont need initialized those fields
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public static final String sf_PathForObjects = "\\.magit\\Objects";
    public static final String sf_PathForBranches = "\\.magit\\Branches";
    public static final String sf_PathForTempFolder = "\\.magit\\Temp";
    public static final String sf_Slash = "\\";
    public static final String sf_txtExtension = ".txt";

    private Path m_RepositoryPath;
    private String m_RepositoryName;
    private Branch m_ActiveBranch;
    private List<Branch> m_Branches = null;
    private Path m_ObjectsFolderPath;
    private Path m_BranchesFolderPath;
    private Path m_TempFolderPath;

    public Repository(Path i_RepositoryPath, String i_RepositoryName, Branch i_ActiveBranch)
    {
        this.m_RepositoryPath = i_RepositoryPath;
        this.m_RepositoryName = i_RepositoryName;
        m_ActiveBranch = i_ActiveBranch;

        if (m_Branches == null)
        {
            m_Branches = new ArrayList<Branch>();
        }

        //m_ActiveBranch = new Branch("Master", null);// once there is a first commit for than the current commit is updated
        m_Branches.add(i_ActiveBranch);
        settingAllPathsInRepository(i_RepositoryPath);

    }

    public Repository(Branch i_ActiveBranch, Path i_RepositoryPath, String i_RepositoryName, List<Branch> i_AllBranches)
    {
        this.m_ActiveBranch = i_ActiveBranch;
        this.m_RepositoryPath = i_RepositoryPath;
        this.m_RepositoryName = i_RepositoryName;
        this.m_Branches = i_AllBranches;

        setAllPathsOfRepositoryDirectories(i_RepositoryPath);
    }

    // this method creates a temp file - writes into it and returns its Path
    public static Path WritingStringInAFile(String i_FileContent, String i_FileName)
    {
        try
        {
            File temp = File.createTempFile(i_FileName, ".txt");
            temp.deleteOnExit();
            Files.write(temp.toPath(), i_FileContent.getBytes());
            return temp.toPath();
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }

    private void settingAllPathsInRepository(Path i_RepositoryPath)
    {
        setAllPathsOfRepositoryDirectories(i_RepositoryPath);
    }

    private void setAllPathsOfRepositoryDirectories(Path i_RepositoryPath)
    {
        String ObjectsPath = (i_RepositoryPath.toString() + sf_PathForObjects);
        m_ObjectsFolderPath = Paths.get(ObjectsPath);
        String branchesPath = (i_RepositoryPath.toString() + sf_PathForBranches);
        m_BranchesFolderPath = Paths.get(branchesPath);
        String TempPath = (i_RepositoryPath.toString() + sf_PathForTempFolder);
        m_TempFolderPath = Paths.get(TempPath);
    }

    public Commit createNewInstanceCommit(User i_User, String i_SHA1PrevCommit, Folder i_RootFolder, String i_CommitMessage)
    {
        Date date = new Date();
        String CommitSha1 = Commit.createSha1ForCommit(i_RootFolder, i_SHA1PrevCommit, i_CommitMessage, i_User, date);
        Commit theNewCommit = new Commit(CommitSha1, i_RootFolder, i_SHA1PrevCommit, i_CommitMessage, i_User, date);
        return theNewCommit;
    }

    public String CreateNewCommitAndUpdateActiveBranch(User i_CurrentUser, String i_CommitMessage) throws Exception
    {
        String differencesBetweenLastAndCurrentCommit = new String();
        if (Folder.isDirEmpty(m_RepositoryPath))
        {
            return String.format("The root folder is Empty - nothing to commit");
        }
        //else - the root folder contains some files

        if (ThereAreNoCmmitsYet())
        {
            createFirstCommitInActiveBranch(i_CurrentUser, i_CommitMessage);
            updateActiveBranchInFileSystemToPointToLatestCommit();
        } else
        {
            CreateANewCommitInActiveBranch(i_CurrentUser, i_CommitMessage, this.m_ActiveBranch.getCurrentCommit().getSHA1());
            if (this.m_ActiveBranch.getPrevCommit() != null)
            {//it means there is no new commit because there were no changes
                differencesBetweenLastAndCurrentCommit = Commit.findDifferences(m_ActiveBranch.getCurrentCommit(), m_ActiveBranch.getPrevCommit());
                System.out.println(differencesBetweenLastAndCurrentCommit);
                updateActiveBranchInFileSystemToPointToLatestCommit();
            }
        }
        return differencesBetweenLastAndCurrentCommit;
    }

    public boolean ThereAreNoCmmitsYet()
    {
        return getLastCommit() == null;
    }

    private void updateActiveBranchInFileSystemToPointToLatestCommit() throws IOException
    {
        Path branchPath = Paths.get(this.m_RepositoryPath.toString() + "\\.magit\\Branches\\" + this.m_ActiveBranch.getBranchName() + ".txt");
        writeToFileEraseTheOldOne(this.m_ActiveBranch.getCurrentCommit().getSHA1(), branchPath);

    }

    //this method deletes the old file if there is one and write the new one
    private void writeToFileEraseTheOldOne(String i_Contnet, Path i_FilePathIncludingName) throws IOException
    {
        if (i_FilePathIncludingName.toFile().exists())
        {
            i_FilePathIncludingName.toFile().delete();
            i_FilePathIncludingName.toFile().createNewFile(); // creating a new "clean" one
        }
        i_FilePathIncludingName.toFile().createNewFile();
        String str = i_Contnet;
        Path path = i_FilePathIncludingName;
        byte[] strToBytes = str.getBytes();

        Files.write(path, strToBytes);
    }

    private Commit getLastCommit()
    {
        return this.m_ActiveBranch.getCurrentCommit();
    }

    private void CreateANewCommitInActiveBranch(User i_CurrentUser, String i_CommitMessage, String i_SHA1PrevCommit) throws Exception
    {
        Map<Path, Item> itemsMapWithPaths = new HashMap<>();
        // 1. create the root folder - current working copy
        Folder WC = Folder.createInstanceOfFolder(m_RepositoryPath, i_CurrentUser, itemsMapWithPaths);

        if (isThereSomethingToCommit(WC))
        {
            commitAllObjectsInAFolder(WC, i_CurrentUser);

            Path pathForWritingWC = WC.WritingFolderAsATextFile();
            zipAndPutInObjectsFolder(pathForWritingWC.toFile(), WC.getSHA1());

            Commit newCommit = createNewInstanceCommit(i_CurrentUser, i_SHA1PrevCommit, WC, i_CommitMessage);


            //putting Commit in objects
            String contentOfCommit = newCommit.CreatingContentOfCommit();
            Path pathForWritingCommit = WritingStringInAFile(contentOfCommit, newCommit.getSHA1());
            zipAndPutInObjectsFolder(pathForWritingCommit.toFile(), newCommit.getSHA1());

            // assign the new commit to be the current commit in the repository
            updateCurrentCommitAndLastCommitInActiveBranch(newCommit);

        } else
        {
            throw new Exception("There is nothing to commit in repository!");
        }
    }

    private void updateCurrentCommitAndLastCommitInActiveBranch(Commit newCommit)
    {
        //1. updateLastCommit
        m_ActiveBranch.SetPrevCommit(m_ActiveBranch.getCurrentCommit());
        //2. updateCurrentCommit
        m_ActiveBranch.SetCurrentCommit(newCommit);


    }

    // this method creats recursivly the working copy as textFiles zipped inside objects as their names are their sha1
    private void createFirstCommitInActiveBranch(User i_CurrentUser, String i_CommitMessage) throws Exception
    {
        CreateANewCommitInActiveBranch(i_CurrentUser, i_CommitMessage, null);
    }

    //this method takes a Folder object and turns it in to a String -
    //than it writes to a file its content=theString
    //returns a Path to the created textFile of the Folder Object
    public void commitAllObjectsInAFolder(Folder i_FolderToCommit, User i_CurrentUser) throws IOException
    {
        //error: might haven't been initialized. if haven't been initialized (in "else") it will be through exception anyway

        Path pathForCommitItem = null;

        for (Item item : i_FolderToCommit.getListOfItems())
        {
            if (item.getTypeOfFile() == Item.TypeOfFile.FOLDER)
            {
                commitAllObjectsInAFolder((Folder) item, i_CurrentUser);

                Folder FolderForCommit = (Folder) item;
                pathForCommitItem = FolderForCommit.WritingFolderAsATextFile();

            } else //item is type of blob
            {
                Blob blobForCommit = (Blob) item;
                pathForCommitItem = WritingStringInAFile(blobForCommit.getContent(), blobForCommit.getSHA1());
            }

            zipAndPutInObjectsFolder(pathForCommitItem.toFile(), item.getSHA1());
        }
    }

    public void zipAndPutInObjectsFolder(File i_File, String i_Sha1) throws IOException
    {
        String pathForSavingFile = m_ObjectsFolderPath.toString();
        try
        {
            zipAFile(i_File, pathForSavingFile, i_Sha1);
        } catch (IOException e)
        {
           throw e;
        }
    }

    private void zipAFile(File i_FileForZip, String i_PathForSavingFile, String i_FileName) throws IOException
    {
        File fileToZip = new File(i_FileForZip.getAbsolutePath());

        FileOutputStream fos = new FileOutputStream(i_PathForSavingFile + "\\" + i_FileName);

        ZipOutputStream zipOut = new ZipOutputStream(fos);
        FileInputStream fis = new FileInputStream(fileToZip);

        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());

        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;

        while ((length = fis.read(bytes)) >= 0)
        {
            zipOut.write(bytes, 0, length);
        }

        zipOut.close();
        fis.close();
        fos.close();
    }

    private boolean isThereSomethingToCommit(Folder i_NewRootFolder)
    {
        if (ThereAreNoCmmitsYet()) // if it is null then it is the first time we are commiting in this repository
        {
            return true;
        } else
        {
            //if it is the root folder has the same sha1 as the new commits root folder then for sure nothing has changed
            boolean isEqualSha1 = (m_ActiveBranch.getCurrentCommit().getRootFolder().getSHA1()).equals(i_NewRootFolder.getSHA1());
            if (isEqualSha1)
            {
                return false;
            } else
                return true;//something has changed. now we need to check what is that has changed
        }
    }

    public Path getRepositoryPath()
    {
        return this.m_RepositoryPath;
    }

    public Branch getActiveBranch()
    {
        return m_ActiveBranch;
    }

    //this method set an active branch, if it is a new one we add it to all branches list
    public void setActiveBranch(Branch i_ActiveBranch)
    {
        this.m_ActiveBranch = i_ActiveBranch;
        for (int i = 0; i < m_Branches.size(); i++)
        {
            if (m_ActiveBranch.getBranchName().equals(m_Branches.get(i).getBranchName()))
            {
                m_Branches.remove(i);
                m_Branches.add(m_ActiveBranch);
            }
        }
    }

    //maybe needed to be deleted - depends on loadBranch function
   /* void ChangeBranch(String i_NameOfBranch)
    {
        // check if this branch already exists - if so - change to it
        Boolean DoesntExist = false;
        Iterator branchIterator = m_Branches.iterator();
        while (branchIterator.hasNext() && !DoesntExist)
        {
            Branch currBranch = (Branch) branchIterator.next();
            if (currBranch.getBranchName().equals(i_NameOfBranch))
            {
                setActiveBranch(currBranch);
                DoesntExist = true;
            }
        }

    }*/

    private void loadBranchFromBranchFolder(String i_NameOfBranch) throws Exception
    {
        File[] allBranches = m_BranchesFolderPath.toFile().listFiles();
        Boolean found = false;
        int i;
        for (i = 0; i < allBranches.length && !found; i++)
        {
            if (allBranches[i].getName().equals(i_NameOfBranch))
                found = true;
        }
        String commitSha1 = getSha1FromBranchFile(allBranches[i]);
        String[] commitsMembersFromFile = getCommitsMembers(commitSha1); // 0 = rootFolder line 1= last commit sha1 2 = messege 3 = DATE 4 = userName
        String[] rootFolderDetails = Item.GetItemsDetails(commitsMembersFromFile[0]);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:SSS");
        Date rootFoldeDate = new Date();
        rootFoldeDate = Item.ParseDateWithFormat(rootFolderDetails[4]);


        Path textFolderPath = Paths.get(m_ObjectsFolderPath.toString() + "\\" + rootFolderDetails[1]);
        Folder rootFolder = Folder.CreateFolderFromTextFolder(textFolderPath.toFile(),
                m_RepositoryPath,
                rootFolderDetails[1],
                new User(rootFolderDetails[3]),
                rootFoldeDate, m_ObjectsFolderPath);

        Commit brnachCommit = createNewInstanceCommit(new User(rootFolderDetails[3]),
                commitsMembersFromFile[1],
                rootFolder,
                rootFolderDetails[2]);
        setActiveBranch(new Branch(i_NameOfBranch, brnachCommit));
    }

    private String[] getCommitsMembers(String commitSha1)
    {
        String[] commitsMembers = new String[5];
        File commitTextFile = getObjectFromObjectsFolder(commitSha1);
        try (BufferedReader br = new BufferedReader(new FileReader(commitTextFile)))
        {
            String line;
            int i = 0;
            while ((line = br.readLine()) != null)
            {
                commitsMembers[i] = (line);
                i++;
            }
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return commitsMembers;
    }

    private File getObjectFromObjectsFolder(String commitSha1)
    {
        File[] allFiles = m_ObjectsFolderPath.toFile().listFiles();
        for (int i = 0; i < allFiles.length; i++)
        {
            if (allFiles[i].getName().equals(commitSha1))
                return allFiles[i];
        }
        return null;
    }

    private String getSha1FromBranchFile(File i_branchFile)
    {
        String commitsSha1 = "";
        try (BufferedReader br = new BufferedReader(new FileReader(i_branchFile)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                commitsSha1 = line;
            }
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return commitsSha1;
    }


    public void replaceActiveBranch(String i_branchName) throws Exception
    {

        //1. create instance of branch
        Path branchPath = Paths.get(m_BranchesFolderPath.toString() + "\\" + i_branchName + ".txt");
        Branch loadedBranch = Branch.createBranchInstanceFromExistBranch(branchPath);

        //2. set active branch to that branch
        setActiveBranch(loadedBranch);

        //3. insert to HEAD.txt (in Branches folder) the name of the branch
        File HEADFile = Paths.get(m_BranchesFolderPath.toString() + "\\HEAD.txt").toFile();
        FileUtils.writeStringToFile(HEADFile, i_branchName, "UTF-8", false);

    }

    private boolean isAlreadyExistsInBranchList(Branch i_LoadedBranch)
    {
        Iterator branchesIterator = m_Branches.iterator();
        boolean isBranchExist = false;
        while (branchesIterator.hasNext())
        {
            Branch currIteratedBranch = (Branch) branchesIterator.next();
            if (currIteratedBranch.equals(i_LoadedBranch))
            {
                isBranchExist = true;
                break;
            }
        }
        return isBranchExist;
    }


  /*  public Path GetBrnachFolderPath() {
        return this.m_BranchesFolderPath;
    }*/


    public Path GetObjectsFolderPath()
    {
        return this.m_ObjectsFolderPath;
    }

    public Folder GetUpdatedWorkingCopy(User i_user) throws Exception
    {
        Map<Path, Item> map = new HashMap<Path, Item>();
        Folder wc = Folder.createInstanceOfFolder(this.m_RepositoryPath, i_user, map);
        return wc;
    }

    public List<Branch> getAllBranches()
    {
        return m_Branches;
    }

    public Path getBranchesFolderPath()
    {
        return m_BranchesFolderPath;
    }

    public void AddingNewBranchInRepository(String i_nameOfNewBranch) throws IOException
    {
        Branch newBranchToAdd = new Branch(i_nameOfNewBranch, m_ActiveBranch.getCurrentCommit());

        m_Branches.add(newBranchToAdd);
        RepositoryWriter.WritingFileByPath(
                m_BranchesFolderPath + sf_Slash + i_nameOfNewBranch + sf_txtExtension,
                m_ActiveBranch.getCurrentCommit().getSHA1()
        );
    }

    public Path GetTempFolderPath()
    {
        return m_TempFolderPath;
    }

    public String getAllBranchesName()
    {
        List<Branch> allBranches = this.m_Branches;
        StringBuilder allBranchesNameBuilder = new StringBuilder();
        allBranchesNameBuilder.append(ANSI_YELLOW + "Branch name = " + ANSI_RESET + m_ActiveBranch.getBranchName() + ANSI_GREEN + "<<-- HEAD Branch\n" + ANSI_RESET);
        for (int i = 0; i < allBranches.size(); i++)
        {
            Branch currBranch = allBranches.get(i);
            if (!currBranch.getBranchName().equals(m_ActiveBranch.getBranchName()))
            {
                allBranchesNameBuilder.append(ANSI_YELLOW + "Branch name = " + ANSI_RESET + currBranch.getBranchName() + "\n");
            }
        }
        return allBranchesNameBuilder.toString();
    }
}


