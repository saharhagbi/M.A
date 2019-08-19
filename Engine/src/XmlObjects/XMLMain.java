package XmlObjects;

import Objects.Folder;
import System.Repository;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.nio.file.Path;

// this class is a builder for a repository from an xml:
// 1) checks that the the "xml" file is valid
// 2) assigns a MagitRepository data member - parsing from the XML file
// 3) assign a Repository data member - parsing from the MagitRepository dataMember
// 4) to get a valid Repository - a)CreateRepositoryFromXml(path to file) -> b)getRepository();
public class XMLMain
{
    private XMLValidate m_XmlValidate = new XMLValidate();
    private XMLParser m_XmlParser = new XMLParser();
    private Repository m_ParsedRepository = null;
    private MagitRepository m_XmlRepository = null;

    public XMLMain()
    {
    }

    public void setXmlRepositoryInXMLParser(MagitRepository i_XmlRepository)
    {
        this.m_XmlRepository = i_XmlRepository;
    }

    public boolean CheckXMLFile(Path i_XmlFilePath) throws Exception
    {
        boolean isXMLRepoAlreadyExist;

        try
        {
            m_XmlValidate.checkExistencesAndXMLExtension(i_XmlFilePath);

            m_XmlRepository = parseFromXmlFileToXmlMagitRepository(i_XmlFilePath);
            m_XmlValidate.setAllObjects(m_XmlRepository);

            m_XmlValidate.validateXmlRepositoryAndAssign(i_XmlFilePath);
            isXMLRepoAlreadyExist = checkIfAnotherRepoInLocation();

        } catch (Exception xmlException)
        {
            throw xmlException;
        }

        return isXMLRepoAlreadyExist;
    }

    public MagitRepository GetXmlRepository()
    {
        return m_XmlRepository;
    }

    public Repository ParseAndWriteXML(MagitRepository i_MagitRepository) throws Exception
    {
        m_XmlParser.setAllObjects(i_MagitRepository);

        // only if it is valid we continue to create an Repository Object
            m_ParsedRepository = m_XmlParser.parseRepositoryFromXmlFile();

        return m_ParsedRepository;
    }


    public MagitRepository parseFromXmlFileToXmlMagitRepository(Path i_pathToXmlRepository) throws JAXBException
    {
        MagitRepository XmlRepositoryToValidate = null;
        JAXBContext jaxbContext;
        Unmarshaller unmarshaller;

        try
        {
            jaxbContext = JAXBContext.newInstance(MagitRepository.class);
            unmarshaller = jaxbContext.createUnmarshaller();
            XmlRepositoryToValidate = (MagitRepository) unmarshaller.unmarshal(i_pathToXmlRepository.toFile());
        } catch (JAXBException xmlException)
        {
            throw xmlException;
        }

        return XmlRepositoryToValidate;
    }

    private boolean checkIfAnotherRepoInLocation()
    {

        if(!Folder.IsFileExist(m_XmlRepository.getLocation()))
            return false;

        File[] filesInRepo = new File(m_XmlRepository.location).listFiles();
        //if there is such file, check if it is repository

        for (File currentFile : filesInRepo)
        {
            if (Folder.IsMagitFolder(currentFile))
                return true;
        }

        return false;
    }
}