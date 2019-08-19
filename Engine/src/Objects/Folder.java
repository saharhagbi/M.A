package Objects;

import System.CompareItems;
import System.User;
import XmlObjects.RepositoryWriter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static Objects.Item.TypeOfFile.BLOB;
import static Objects.Item.TypeOfFile.FOLDER;
import static System.Repository.WritingStringInAFile;


public class Folder extends Item
{

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";

    List<Item> m_ListOfItems;
    Map<Path, Item> m_MapOfItems;



    public Folder(List<Item> m_ListOfItems, String i_FolderPath, String i_FolderName,
                  String i_SHA1, TypeOfFile i_TypeOfFile, User i_UserName, Date i_Date)
    {
        super(Paths.get(i_FolderPath), i_SHA1, i_TypeOfFile, i_UserName, i_Date, i_FolderName);

        this.m_ListOfItems = m_ListOfItems;
        m_MapOfItems = new HashMap<Path, Item>();
        createMapOfItems();
    }


    public static String FinedDifferences(Folder i_newRootFolder, Folder i_OldRootFolder)
    {
        StringBuilder changesBetweenFolders = new StringBuilder();
        Iterator keyIterator = i_newRootFolder.m_MapOfItems.keySet().iterator();
        Map<Path, Item> modifiableNewFolderMap = new HashMap<Path, Item>(i_newRootFolder.m_MapOfItems);
        Map<Path, Item> modifiableOldFolderMap = new HashMap<Path, Item>(i_OldRootFolder.m_MapOfItems);
        while (keyIterator.hasNext())
        {
            Path keyPath = (Path) keyIterator.next();
            Item newFolderItem = modifiableNewFolderMap.get(keyPath);
            Item oldFolderItem = modifiableOldFolderMap.get(keyPath);
            if (modifiableOldFolderMap.containsKey(keyPath))
            { // exist in the old version of this folder - need to check if its the same or been modified

                if (!newFolderItem.getSHA1().equals(oldFolderItem.getSHA1()))
                {
                    if (newFolderItem.getTypeOfFile() == BLOB)// Item is a blob
                    {
                        changesBetweenFolders.append(ANSI_YELLOW + "File changed : " + ANSI_RESET + keyPath.toString() + '\n');
                    } else//Item is a folder
                    {
                        changesBetweenFolders.append(ANSI_YELLOW + "Folder changed: " + ANSI_RESET + keyPath.toString() + '\n');
                        String changesInsideThisItemFolder = FinedDifferences((Folder) newFolderItem, (Folder) oldFolderItem);
                        changesBetweenFolders.append(changesInsideThisItemFolder);
                    }
                    modifiableOldFolderMap.remove(keyPath);// remove item so we keep searching for changes
                }
            } else
            {
                if (newFolderItem.getTypeOfFile() == BLOB)
                {
                    changesBetweenFolders.append(ANSI_GREEN + "File added : " + ANSI_RESET + newFolderItem.GetPath().toString() + '\n');
                } else
                {
                    changesBetweenFolders.append(ANSI_GREEN + "Folder added : " + ANSI_RESET + newFolderItem.GetPath().toString() + '\n');
                    String addedItemsInsideThisItemFolder = getAddedFiles((Folder) newFolderItem);
                    changesBetweenFolders.append(addedItemsInsideThisItemFolder);
                }

            }
            //was the same so we dont want to mention it anymore
            modifiableNewFolderMap.remove(keyPath);
            modifiableOldFolderMap.remove(keyPath);

        }
        Iterator oldFolderKeysIterator = modifiableOldFolderMap.keySet().iterator();
        while (oldFolderKeysIterator.hasNext())
        {
            Path keyPath = (Path) oldFolderKeysIterator.next();
            Item item = (Item) modifiableOldFolderMap.get(keyPath);
            if (item.getTypeOfFile() == BLOB)
            {
                changesBetweenFolders.append(ANSI_RED + "File removed : " + ANSI_RESET + keyPath.toString() + System.lineSeparator());
            } else
            {
                changesBetweenFolders.append(ANSI_RED + "Folder removed : " + ANSI_RESET + keyPath.toString() + System.lineSeparator());
            }
        }
        return changesBetweenFolders.toString();
    }

    private static String getAddedFiles(Folder i_FolderThatHasBeenAdded)
    {
        StringBuilder addedItems = new StringBuilder();
        Iterator keyIterator = i_FolderThatHasBeenAdded.m_MapOfItems.keySet().iterator();
        while (keyIterator.hasNext())
        {
            Item item = i_FolderThatHasBeenAdded.m_MapOfItems.get(keyIterator.next());
            if (item.getTypeOfFile() == BLOB)
            {
                addedItems.append("File added : " + item.GetPath().toString() + System.lineSeparator());
            } else
            {
                addedItems.append("Folder added : " + item.GetPath().toString() + System.lineSeparator());
                String insideFolderAddedItems = getAddedFiles((Folder) item);//recursive call for this item which is a folder
                addedItems.append(insideFolderAddedItems);
            }
        }
        return addedItems.toString();
    }

    public static String GetInformation(Folder i_Folder)
    {
        StringBuilder folderDetailsBuilder = new StringBuilder();
        folderDetailsBuilder.append("Folder:\n");
        folderDetailsBuilder.append("Name: " + i_Folder.GetPath().toString() + "\n");
        folderDetailsBuilder.append("Type: " + i_Folder.getTypeOfFile().toString() + "\n");
        folderDetailsBuilder.append("Sah1: " + i_Folder.getSHA1() + "\n");
        folderDetailsBuilder.append("Changed by : " + i_Folder.getUser().getUserName() + "\n");
        folderDetailsBuilder.append("Time changed : " + Item.getDateStringByFormat(i_Folder.getDate()) + "\n");
        List<Item> folderListOfItems = i_Folder.m_ListOfItems;
        for (int i = 0; i < folderListOfItems.size(); i++)
        {
            if (folderListOfItems.get(i).getTypeOfFile().equals(FOLDER))
            {
                folderDetailsBuilder.append(GetInformation((Folder) folderListOfItems.get(i)));
            } else
            {
                StringBuilder blobDetailsBuilder = new StringBuilder();
                blobDetailsBuilder.append("Blob:\n");
                blobDetailsBuilder.append("Name: " + folderListOfItems.get(i).GetPath().toString() + "\n");
                blobDetailsBuilder.append("Type: " + folderListOfItems.get(i).getTypeOfFile().toString() + "\n");
                blobDetailsBuilder.append("Sah1: " + folderListOfItems.get(i).getSHA1() + "\n");
                blobDetailsBuilder.append("Changed by : " + folderListOfItems.get(i).getUser().getUserName() + "\n");
                blobDetailsBuilder.append("Time changed : " + Item.getDateStringByFormat(folderListOfItems.get(i).getDate()) + "\n");
                folderDetailsBuilder.append(blobDetailsBuilder.toString());
            }

        }
        return folderDetailsBuilder.toString();
    }

    public static void SpanDirectory(Folder i_RootFolder) throws IOException
    {
        Path folderPath = i_RootFolder.GetPath();
        folderPath.toFile().mkdir();
        Iterator itemsIterator = i_RootFolder.m_ListOfItems.iterator();
        while (itemsIterator.hasNext())
        {
            Item folderItem = (Item) itemsIterator.next();
            if (folderItem.getTypeOfFile().equals(FOLDER))
            {
                SpanDirectory((Folder) folderItem);
            } else
            {
                Blob currentBlob = (Blob) folderItem;
                RepositoryWriter.WritingFileByPath(currentBlob.GetPath().toString(), currentBlob.getContent());
            }
        }
    }

    public static void RemoveFilesAndFoldersWithoutMagit(Path i_FolderPathToRemove) throws IOException
    {
        File[] rootFolder = new File(i_FolderPathToRemove.toString()).listFiles();

        for (File currentFile : rootFolder)
        {
            if (!currentFile.getName().equals(".magit"))
            {
                if (currentFile.isDirectory() == true)
                {
                    FileUtils.deleteDirectory(currentFile);
                } else
                {
                    currentFile.delete();
                }
            }
        }
    }

    public static Folder CreateFolderFromTextFolder(File i_TextFolder, Path i_PathToThisFolder, String i_foldersSha1, User i_user, Date i_Date, Path i_ObjectsFolderPath) throws Exception
    {   //example: 123,50087888a7c34344416ec0fd600f394dadf3d9d8,FOLDER,Administrator,06.39.2019-06:39:27:027
        Path tempFolderPath = Paths.get(i_ObjectsFolderPath.getParent().toString() + "\\Temp");
        if (!tempFolderPath.toFile().exists())
        {
            tempFolderPath.toFile().mkdir();
        }
        List<Item> folderListOfItems = new ArrayList<Item>();
        String FoldersName = i_PathToThisFolder.getFileName().toString();
        Path foldersPath = i_PathToThisFolder;
        String foldersSha1 = i_foldersSha1;
        TypeOfFile type = FOLDER;
        User user = i_user;
        Date date = i_Date;
        try
        {
            Scanner lineScanner = new Scanner(i_TextFolder);
            while (lineScanner.hasNext())
            {
                String lineOfDetails = lineScanner.nextLine();
                if (Item.IsAFile(lineOfDetails))
                {
                    // a line that represent a blob, hence we parse it accordingly
                    Blob thisBlob;
                    String[] itemsDetails = Item.GetItemsDetails(lineOfDetails);
                    Path filePath = Paths.get(i_PathToThisFolder.toString() + "\\" + itemsDetails[0]);
                    String fileSha1 = itemsDetails[1];
                    User fileUser = new User(itemsDetails[3]);
                    Date fileDate = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:SSS");
                    fileDate = Item.ParseDateWithFormat(itemsDetails[4]);

                    //Blob(File i_File, String i_Sha1, String i_FileContent, TypeOfFile i_TypeOfFile, User i_CurrentUser, String i_BlobName)
                    Path zippedBlobPathInObjectsFolder = Paths.get(i_ObjectsFolderPath.toString() + "\\" + fileSha1);
                    Path tempBlobFile = Item.UnzipFile(zippedBlobPathInObjectsFolder, tempFolderPath);

                    String blobContent = Blob.getFileContent(tempBlobFile.toFile());
                    //Blob(File i_File, String i_Sha1, String i_FileContent, TypeOfFile i_TypeOfFile, User i_CurrentUser, String i_BlobName) {
                    thisBlob = new Blob(filePath, fileSha1, blobContent, BLOB, fileUser, fileDate, filePath.getFileName().toString());
                    folderListOfItems.add(thisBlob);


                } else
                {//if it a folder
                    //123, 50087888a7c34344416ec0fd600f394dadf3d9d8, FOLDER, Administrator, 06.39.2019-06:39:27:027
                    String[] folderDetails = Item.GetItemsDetails(lineOfDetails);
                    String nameOfFolder = folderDetails[0];
                    Path folderZippedInObjectsFolderPath = Paths.get(i_ObjectsFolderPath.toString() + "\\" + folderDetails[1]);
                    Path tempTextFolderPathUnzipped = Item.UnzipFile(folderZippedInObjectsFolderPath, tempFolderPath);
                    User folderUser = new User(folderDetails[3]);
                    Date fileDate = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:SSS");
                    fileDate = Item.ParseDateWithFormat(folderDetails[4]);
                    Path FolderPathInRootFolder = Paths.get(i_PathToThisFolder.toString() + "\\" + nameOfFolder);
                    Folder currentItemFolder = CreateFolderFromTextFolder(tempTextFolderPathUnzipped.toFile(), FolderPathInRootFolder, folderDetails[1], folderUser, fileDate, i_ObjectsFolderPath);
                    folderListOfItems.add(currentItemFolder);
                }

            }
        } catch (Exception e)
        {
            throw e;
        }

        return new Folder(folderListOfItems,
                i_PathToThisFolder.toString(),
                FoldersName,
                i_foldersSha1,
                FOLDER,
                user,
                date);

    }

    public static Folder createInstanceOfFolder(Path i_FolderPath, User i_CurrentUser, Map<Path, Item> i_ItemsMapWithPaths) throws Exception
    {
        List<Item> allItemsInCurrentFolder = new ArrayList<>();

        File[] files = new File(i_FolderPath.toString()).listFiles();
        for (File file : files)
        {
            if (!IsMagitFolder(file))
            {
                if (file.isDirectory())
                {
                    Folder currentFolderReturnedFromRecursion = createInstanceOfFolder(file.toPath(), i_CurrentUser, i_ItemsMapWithPaths);
                    allItemsInCurrentFolder.add(currentFolderReturnedFromRecursion);
                } else
                {
                    String sha1 = createSHA1ForTextFile(file);
                    Blob currentBlob = new Blob(file.toPath(), sha1, Blob.ReadLineByLine(file),
                            Item.TypeOfFile.BLOB, i_CurrentUser, new Date(), file.getName());
                    allItemsInCurrentFolder.add(currentBlob);
                }
            }
        }
        for (int i = 0; i < allItemsInCurrentFolder.size(); i++)
        {
            i_ItemsMapWithPaths.put(allItemsInCurrentFolder.get(i).GetPath(), allItemsInCurrentFolder.get(i));
        }
        Folder currentFolder = new Folder(allItemsInCurrentFolder, i_FolderPath.toString(), i_FolderPath.getFileName().toString(),
                CreateSHA1ForFolderFile(allItemsInCurrentFolder), Item.TypeOfFile.FOLDER, i_CurrentUser, new Date());

        return currentFolder;

    }

    public static boolean IsMagitFolder(File file)
    {
        return file.getName().equals(".magit");
    }

    public static String CreateSHA1ForFolderFile(List<Item> i_AllItems)
    {
        StringBuilder stringForCreatingSHA1 = new StringBuilder();
        Collections.sort(i_AllItems, new CompareItems());
        for (Item currentItemInList : i_AllItems)
        {
            stringForCreatingSHA1.append(currentItemInList.getName());
            stringForCreatingSHA1.append(currentItemInList.getSHA1());
            stringForCreatingSHA1.append(currentItemInList.getTypeOfFile().toString());
        }

        return DigestUtils.sha1Hex(stringForCreatingSHA1.toString());
    }

    private static String createSHA1ForTextFile(File i_File) throws Exception
    {
        String stringForSha1 = Blob.ReadLineByLine(i_File);

        return DigestUtils.sha1Hex(stringForSha1);
    }

    public static boolean isDirEmpty(final Path directory) throws IOException
    {

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory))
        {
            return !dirStream.iterator().hasNext();
        }
    }

    public static void DeleteDirectory(String i_LocationOfFolderToDelete) throws IOException
    {
        Path pathOfFolderToDelete = Paths.get(i_LocationOfFolderToDelete);
        FileUtils.deleteDirectory(pathOfFolderToDelete.toFile());
    }

    public static boolean IsFileExist(String i_LocationOfFile)
    {
        File tempFileForCheckExistence = new File(i_LocationOfFile);

        return tempFileForCheckExistence.exists();
    }


    //should replace createTempCommitWithoutCreatingObjects() if possible

    private void createMapOfItems()
    {
        Iterator itemsIterator = m_ListOfItems.iterator();
        while (itemsIterator.hasNext())
        {
            Item item = (Item) itemsIterator.next();
            m_MapOfItems.put(item.GetPath(), item);
        }
    }

    public Integer GetAmountOfItems()
    {
        return m_ListOfItems.size();
    }

    public List<Item> getListOfItems()
    {
        return m_ListOfItems;
    }

    public Path WritingFolderAsATextFile()
    {
        Path pathFileForWritingString;

        String fileContent = convertFolderToString(this, this.getUser());

        pathFileForWritingString = WritingStringInAFile(fileContent, this.getSHA1());

        return pathFileForWritingString;
    }

    private String convertFolderToString(Folder i_Folder, User i_CurrentUser)
    {
        StringBuilder resultString = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:SSS");

        for (Item item : i_Folder.getListOfItems())
        {
            resultString.append(item.getName() + ",");
            resultString.append(item.getSHA1() + ",");
            resultString.append(item.getTypeOfFile().toString() + ",");
            resultString.append(i_CurrentUser.getUserName() + ",");
            resultString.append(dateFormat.format(item.getDate()) + "\n");
        }

        return resultString.toString();
    }

}
