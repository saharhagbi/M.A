package XmlObjects;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XMLValidate
{
    public static final String BLOB_TYPE = "blob";
    public static final String FOLDER_TYPE = "folder";

    private MagitRepository m_MagitRepository;
    private MagitBlobs m_MagitBlobs;
    private MagitFolders m_MagitFolders;
    private MagitCommits m_MagitCommits;

    public XMLValidate()
    {
    }

    public void setAllObjects(MagitRepository i_MagitRepository)
    {
        this.m_MagitRepository = i_MagitRepository;
        this.m_MagitBlobs = i_MagitRepository.magitBlobs;
        this.m_MagitFolders = i_MagitRepository.magitFolders;
        this.m_MagitCommits = i_MagitRepository.magitCommits;
    }
/*

    public XMLValidate(MagitRepository i_MagitRepository)
    {
        this.m_MagitRepository = i_MagitRepository;
        this.m_MagitBlos = i_MagitRepository.magitBlobs;
        this.m_MagitFolders = i_MagitRepository.magitFolders;
        this.m_MagitCommits = i_MagitRepository.magitCommits;
    }
*/

    public boolean validateXmlRepositoryAndAssign(Path i_PathToXmlRepositoryFile) throws Exception
    {
        // 1) check its extension and existence
        //checkExistencesAndXMLExtension(i_PathToXmlRepositoryFile);

        XMLMain xmlMain = new XMLMain();
        m_MagitRepository = xmlMain.parseFromXmlFileToXmlMagitRepository(i_PathToXmlRepositoryFile);
        xmlMain.setXmlRepositoryInXMLParser(m_MagitRepository);

        // 2) check that no MagitBlob in MagitBlobs has same ID as other - same check for MagitCommits,MagitFolder
        doesMagitObjectsHaveSameId();

        // 3) inside every MagitFolder check that a MagitBlob that is pointed with Id really Exists
        doesAllPointedMagitBlobsExists();

        // 4)inside every MagitFolder check that a MagitFolder that is pointed with Id really Exists
        doesAllPointedMagitFolderExists();

        // 5) check that A folder doesnt points to itself - by id
        doesAMagitFolderPointsToItSelf();

        // 6) check that the Folder pointed in all MagitCommit is a MagitFolder that exsits in MagitFolders
        List<MagitSingleFolder> listToRemoveFoldersForCheckingIsRoot = new ArrayList<MagitSingleFolder>(m_MagitFolders.magitSingleFolder);
        doesAllMagitFolderInMagitCommitsExistsInMagitFoldersAndIsRootIsTrue(listToRemoveFoldersForCheckingIsRoot);

        // 7) check that every MagitFolder that is beeing pointed by MagitCommit is a rootFoler aka: is-root="true"
        checkAllMagitSingleFoldersThatAreNotRoots(listToRemoveFoldersForCheckingIsRoot);

        // 8) check that every MagitCommit beeing pointed by MagitBranch is defined
        doesAllMagitBranchesPointsToDefinedMagitCommit();

        // 9) check that HEAD Points to a name of MagitBranch that is defined
        doesHeadPointsToDefinedNameOfMagitBranch();

        //if XmlRepositoryToValidate has passed all validations than we assign to m_XmlRepository
        //then return true

        return true;
    }

    private void checkAllMagitSingleFoldersThatAreNotRoots(List<MagitSingleFolder> i_ListMagitSingleFolderWithNoRoots) throws Exception
    {
        for (MagitSingleFolder magitSingleFolder : i_ListMagitSingleFolderWithNoRoots)
        {
            if (isRootFolderValid(magitSingleFolder))
                throw new Exception("Magit folder is not root although it's value, is root, equivalent to true");

        }
    }

    private void doesHeadPointsToDefinedNameOfMagitBranch() throws Exception
    {
        boolean headFounded = false;
        for (MagitSingleBranch magitSingleBranch : m_MagitRepository.magitBranches.getMagitSingleBranch())
        {
            if (m_MagitRepository.magitBranches.head.equals(magitSingleBranch.name))
                headFounded = true;
        }

        if (!headFounded)
            throw new Exception("Head pointed to commit that is not defined");
    }

    private void doesAllMagitBranchesPointsToDefinedMagitCommit() throws Exception
    {
        boolean isFound = false;

        for (MagitSingleBranch magitSingleBranch : m_MagitRepository.magitBranches.getMagitSingleBranch())
        {
            for (MagitSingleCommit magitSingleCommit : m_MagitCommits.getMagitSingleCommit())
            {
                if (magitSingleBranch.pointedCommit.id.equals(magitSingleCommit.id))
                {
                    isFound = true;
                    break;
                }
            }
            if (!isFound)
                throw new Exception("Magit branch pointed to commit not defined");

            isFound = false;
        }
    }

    private void doesAllMagitFolderInMagitCommitsExistsInMagitFoldersAndIsRootIsTrue(List<MagitSingleFolder> i_ListMagitSingleFolder) throws Exception
    {
        boolean isFound = false;

        for (MagitSingleCommit magitSingleCommit : m_MagitCommits.getMagitSingleCommit())
        {
            for (MagitSingleFolder magitSingleFolder : i_ListMagitSingleFolder)
            {
                if ((magitSingleCommit.rootFolder.id.equals(magitSingleFolder.id)))
                {
                    if (isRootFolderValid(magitSingleFolder))
                    {
                        isFound = true;
                        i_ListMagitSingleFolder.remove(magitSingleFolder);
                        break;
                    } else
                    {
                        throw new Exception("isRoot value, of Magit folder(root folder) pointed by commit, is false");
                    }
                }
            }
            if (!isFound)
            {
                throw new Exception("There is not root folder pointed by commit");
            }

            isFound = false;
        }
    }

    private Boolean isRootFolderValid(MagitSingleFolder magitSingleFolder)
    {
        return (magitSingleFolder.isRoot != null) && (magitSingleFolder.isRoot == true);
    }

    private void doesAMagitFolderPointsToItSelf() throws Exception
    {
        for (MagitSingleFolder magitSingleFolder : m_MagitFolders.getMagitSingleFolder())
        {
            for (Item item : magitSingleFolder.getItems().getItem())
            {
                if (isTheSameItem(item, FOLDER_TYPE))
                {
                    if (areFolderAndItemHaveSameId(magitSingleFolder, item))
                        throw new Exception("Magit folder point to itself");
                }
            }
        }
    }

    private boolean isTheSameItem(Item i_Item, String i_ItemStringToValidate)
    {
        return i_Item.type.equals(i_ItemStringToValidate);
    }

    private boolean areFolderAndItemHaveSameId(MagitSingleFolder i_MagitSingleFilder, Item i_Item)
    {
        return i_MagitSingleFilder.id.equals(i_Item.id);
    }

    private void doesAllPointedMagitFolderExists() throws Exception
    {
        for (MagitSingleFolder singleFolder : m_MagitFolders.getMagitSingleFolder())
        {
            for (Item item : singleFolder.getItems().getItem())
            {
                if (isTheSameItem(item, FOLDER_TYPE))
                {
                    if (!folderExist(item))
                        throw new Exception("Magit folder pointed doesn't exist");
                }
            }
        }
    }

    private boolean folderExist(Item i_Item)
    {
        for (MagitSingleFolder folder : m_MagitFolders.getMagitSingleFolder())
        {
            if (areFolderAndItemHaveSameId(folder, i_Item))
                return true;
        }

        return false;
    }

    private void doesAllPointedMagitBlobsExists() throws Exception
    {
        for (MagitSingleFolder singleFolder : m_MagitFolders.getMagitSingleFolder())
        {
            for (Item item : singleFolder.getItems().getItem())
            {
                if (isTheSameItem(item, BLOB_TYPE))
                {
                    if (!blobExist(item))
                        throw new Exception("Magit blob pointed doesn't exist");
                }
            }
        }
    }

    private boolean blobExist(Item i_BlobToCheck)
    {
        for (MagitBlob blob : m_MagitBlobs.getMagitBlob())
        {
            if (blob.id.equals(i_BlobToCheck.id))
                return true;
        }

        return false;
    }

    private <T> void checkObjectsHaveSameID(List<T> i_ObjectsToCheck) throws Exception
    {
        Iterator<T> Yitem;

        for (T Xitem : i_ObjectsToCheck)
        {
            Yitem = i_ObjectsToCheck.listIterator(i_ObjectsToCheck.indexOf(Xitem));
            Yitem.next();
            while (Yitem.hasNext())
            {
                if (twoElementsHaveSameID(Xitem, Yitem.next()))
                    throw new Exception("Two elements have same id");
            }
        }
    }

    private <T> boolean twoElementsHaveSameID(T i_XItem, T i_YItem)
    {
        if (i_XItem.getClass() == MagitSingleFolder.class)
        {
            MagitSingleFolder tempXItem = (MagitSingleFolder) i_XItem;
            MagitSingleFolder tempYItem = (MagitSingleFolder) i_YItem;

            return tempXItem.id.equals(tempYItem.id);

        } else if (i_XItem.getClass() == MagitBlob.class)
        {
            MagitBlob tempXItem = (MagitBlob) i_XItem;
            MagitBlob tempYItem = (MagitBlob) i_YItem;

            return tempXItem.id.equals(tempYItem.id);

        } else
        {
            MagitSingleCommit tempXItem = (MagitSingleCommit) i_XItem;
            MagitSingleCommit tempYItem = (MagitSingleCommit) i_YItem;

            return tempXItem.id.equals(tempYItem.id);
        }
    }

    private String getFileExtension(File i_File)
    {
        String name = i_File.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1)
        {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf);
    }

    public void checkExistencesAndXMLExtension(Path i_PathToXmlRepository) throws Exception
    {
        if (!i_PathToXmlRepository.toFile().exists())
            throw new Exception("File doesn't exist");


        String extension = getFileExtension(i_PathToXmlRepository.toFile());
        if (!extension.toUpperCase().equals(".XML"))
            throw new Exception("File extension is not XML");
    }

    private void doesMagitObjectsHaveSameId() throws Exception
    {
        checkObjectsHaveSameID(m_MagitBlobs.getMagitBlob());
        checkObjectsHaveSameID(m_MagitFolders.getMagitSingleFolder());
        checkObjectsHaveSameID(m_MagitCommits.getMagitSingleCommit());
    }
}
