import org.aphronatus.msbt.MSBT
import org.aphronatus.msbt.MSBTUtils
import spock.lang.Specification
import spock.lang.Unroll

class FileTestSpec extends Specification {

    static PATH_MSBT_EXAMPLES = "src/test/resources/msbt_examples/"
    static PATH_MSBT_DUMP = "src/test/resources/dump/"
    static PATH_MSBT_YAML = "src/test/resources/yaml/"

    @Unroll
    void "test file: #file"() {
        setup:
        String originalMsbt = PATH_MSBT_EXAMPLES + file
        File originalMSBTFile = new File(originalMsbt)

        when:
        MSBT msbtOriginal = MSBTUtils.readFromMSBTFile(originalMSBTFile)
        File tempYAMLFile = File.createTempFile("temp", ".yaml")

        then:
        MSBTUtils.writeMSBTYAMLFile(msbtOriginal, tempYAMLFile)

        then: 'test yaml with example yaml'
        true


        when: 'write yaml to msbt'
        File tempMSBTFile = File.createTempFile("temp", ".msbt")
        MSBT msbtFromYAML = MSBTUtils.readFromMSBTYAMLFile(tempYAMLFile)
        MSBTUtils.writeMSBTFile(msbtFromYAML, tempMSBTFile)

        then: 'compare original msbt and new msbt'

        then: 'compare original msbt and new msbt'
        def hexOriginal = toHex(originalMSBTFile.bytes)
        def hexNew = toHex(tempMSBTFile.bytes)


        for (i in 0..<hexOriginal.size()) {
            if (hexOriginal[i] != hexNew[i]) {
                // write yaml and msbt dump
                tempMSBTFile.renameTo(new File(PATH_MSBT_DUMP + file + ".msbt"))
                tempYAMLFile.renameTo(new File(PATH_MSBT_DUMP + file + ".yaml"))

                throw new Exception("Not equal in byte ${i} ${hexOriginal[i]} != ${hexNew[i]}")
            }
        }

        // if success write yaml in yaml folder
        def yamlFileFinal = new File(PATH_MSBT_YAML + file + ".yaml")

        // create folder if not exists
        if (!yamlFileFinal.parentFile.exists()) {
            yamlFileFinal.parentFile.mkdirs()
        }

        tempYAMLFile.renameTo(yamlFileFinal)

        cleanup:
        tempYAMLFile.delete()
        tempMSBTFile.delete()

        where:
        file << getFileList(PATH_MSBT_EXAMPLES)
    }

    static List<String> getFileList(String folder) {
        List<String> filesNames = getAllFilesInFolder(folder)
        return filesNames
    }

    static List<String> getAllFilesInFolder(String folder) {
        List<String> files = new ArrayList<>()
        File folderFile = new File(folder)

        if (folderFile.isDirectory()) {
            for (File file in folderFile.listFiles()) {
                if (file.isDirectory()) {
                    List<String> filesInFolder = getAllFilesInFolder(file.absolutePath)
                    files.addAll(filesInFolder.collect { file.name + "/" + it })
                } else {
                    files.add(file.name)
                }
            }
        }

        return files
    }

    String[] toHex(byte[] bytes) {
        String[] hexArray = new String[bytes.length]

        for (int i = 0; i < bytes.length; i++) {
            hexArray[i] = String.format("%02X", bytes[i])
        }

        return hexArray
    }
}
