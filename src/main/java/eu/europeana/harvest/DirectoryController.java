package eu.europeana.harvest;

import java.io.File;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-03-27
 */
public class DirectoryController {
    private int entriesCounterCurrentDirectory;
    private int totalFilesCounterInDirectoryTree;
    private final int maxEntriesInDirectory;
    private final File rootDirectory;
    private File currentDirectory;
    private int treeDepth = 2;

    public DirectoryController(int maxEntriesInDirectory, File rootDirectory) {
        this.maxEntriesInDirectory = maxEntriesInDirectory;
        this.rootDirectory = rootDirectory;
        this.currentDirectory = new File(this.rootDirectory, "1-" + maxEntriesInDirectory);
    }

    public File getFileToWriteOnDirectoryStructure(int offset, int limit)
    {
        if(entriesCounterCurrentDirectory <= maxEntriesInDirectory)
        {
            entriesCounterCurrentDirectory++;
            totalFilesCounterInDirectoryTree++;
            File file = new File(currentDirectory, "request." + offset + "-" + limit + ".txt");
            return file;
        }
        else
        {
            double maxFilesPerDepth = Math.pow(maxEntriesInDirectory, treeDepth);
            if(totalFilesCounterInDirectoryTree <= maxFilesPerDepth)
            {
                currentDirectory = new File(currentDirectory.getParent(), totalFilesCounterInDirectoryTree + "-"
                        + (totalFilesCounterInDirectoryTree - 1 + maxFilesPerDepth/maxEntriesInDirectory));
                entriesCounterCurrentDirectory = 1;
//                totalFilesCounterInDirectoryTree++;
                File file = new File(currentDirectory, "request." + offset + "-" + limit + ".txt");
                return file;
            }
            else
            {
                // TODO: 03.03.17 Move all directories to a temp directory
                treeDepth++;
                maxFilesPerDepth = Math.pow(maxEntriesInDirectory, treeDepth);
                currentDirectory = new File(currentDirectory.getParent(), "1-"
                        + (maxFilesPerDepth / maxEntriesInDirectory));
                // TODO: 03.03.17 Move all above directories back in the currentDirectory
                currentDirectory = new File(currentDirectory.getParent(), totalFilesCounterInDirectoryTree + "-"
                        + (totalFilesCounterInDirectoryTree - 1 + maxFilesPerDepth/maxEntriesInDirectory));

                int currentDepth = treeDepth;
                while( currentDepth > 2)
                {
                    currentDepth--;
                    maxFilesPerDepth = Math.pow(maxEntriesInDirectory, currentDepth);
                    currentDirectory = new File(currentDirectory, totalFilesCounterInDirectoryTree + "-"
                            + (totalFilesCounterInDirectoryTree - 1 + maxFilesPerDepth/maxEntriesInDirectory));
                }
                File file = new File(currentDirectory, "request." + offset + "-" + limit + ".txt");
                return file;
            }
        }

    }
}
