package us.ihmc.pathPlanning.visibilityGraphs.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import us.ihmc.euclid.tools.EuclidCoreIOTools;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.PlanarRegionFileTools;
import us.ihmc.robotics.geometry.PlanarRegionsList;

public class VisibilityGraphsIOTools
{
   protected static final boolean DEBUG = true;

   public static final String INPUTS_PARAMETERS_FILENAME = "VizGraphsInputs.txt";

   private static final String PATH_SIZE_FIELD_OPEN = "<PathSize,";
   private static final String PATH_SIZE_FIELD_END = ",PathSize>";

   protected static final String START_FIELD_OPEN = "<Start,";
   protected static final String START_FIELD_CLOSE = ",Start>";

   protected static final String GOAL_FIELD_OPEN = "<Goal,";
   protected static final String GOAL_FIELD_END = ",Goal>";

   public static final String TESTABLE_FLAG = "testVisGraph";

   protected static String getPoint3DString(Point3DReadOnly point3D)
   {
      return EuclidCoreIOTools.getStringOf("", "", ",", point3D.getX(), point3D.getY(), point3D.getZ());
   }

   private static Point3D parsePoint3D(String stringPoint3D)
   {
      double x = Double.parseDouble(stringPoint3D.substring(0, stringPoint3D.indexOf(",")));
      stringPoint3D = stringPoint3D.substring(stringPoint3D.indexOf(",") + 1);
      double y = Double.parseDouble(stringPoint3D.substring(0, stringPoint3D.indexOf(",")));
      stringPoint3D = stringPoint3D.substring(stringPoint3D.indexOf(",") + 1);
      double z = Double.parseDouble(stringPoint3D.substring(0));

      return new Point3D(x, y, z);
   }

   protected static <T> T parseField(BufferedReader file, String fieldOpen, String fieldClose, Parser<T> parser)
   {
      T ret = null;

      try
      {
         String sCurrentLine;

         while ((sCurrentLine = file.readLine()) != null)
         {
            if (sCurrentLine.contains(fieldOpen) && sCurrentLine.contains(fieldClose))
            {
               ret = parser.parse(sCurrentLine.substring(fieldOpen.length(), sCurrentLine.indexOf(fieldClose)));
               break;
            }
         }

         file.reset();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }

      return ret;
   }

   protected static int parsePathSize(BufferedReader file)
   {
      Integer pathSize = parseField(file, PATH_SIZE_FIELD_OPEN, PATH_SIZE_FIELD_END, Integer::valueOf);
      if (pathSize == null)
         return -1;
      else
         return pathSize.intValue();
   }

   protected static Point3D parseStartField(BufferedReader bufferedReader)
   {
      return parseField(bufferedReader, START_FIELD_OPEN, START_FIELD_CLOSE, VisibilityGraphsIOTools::parsePoint3D);
   }

   protected static Point3D parseGoalField(BufferedReader bufferedReader)
   {
      return parseField(bufferedReader, GOAL_FIELD_OPEN, GOAL_FIELD_END, VisibilityGraphsIOTools::parsePoint3D);
   }

   protected static void writeField(File file, String fieldOpen, String fieldClose, Writer writer)
   {
      BufferedWriter bw = null;

      try
      {
         if (!file.exists())
            file.createNewFile();

         FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
         bw = new BufferedWriter(fw);

         bw.write(fieldOpen + writer.getStringToWrite() + fieldClose);
         bw.newLine();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      } finally
      {
         try
         {
            if (bw != null)
               bw.close();
         }
         catch (IOException ex)
         {
            ex.printStackTrace();
         }
      }
   }

   protected static interface Parser<T>
   {
      T parse(String string);
   }

   protected static interface Writer
   {
      String getStringToWrite();
   }

   public static String[] getPlanarRegionAndVizGraphsFilenames(File parentFolder)
   {
      if (!parentFolder.exists() || !parentFolder.isDirectory())
         return null;

      return Arrays.stream(parentFolder.listFiles(file -> isVisibilityGraphsDataset(file) || PlanarRegionFileTools.isPlanarRegionFile(file))).map(File::getName)
                   .toArray(String[]::new);
   }

   public static boolean isVisibilityGraphsDataset(File dataFolder)
   {
      if (dataFolder == null || !dataFolder.exists() || !dataFolder.isDirectory())
         return false;

      File[] paramsFiles = dataFolder.listFiles((dir, name) -> name.equals(INPUTS_PARAMETERS_FILENAME));

      if (paramsFiles == null || paramsFiles.length != 1)
         return false;

      File[] planarRegionFolders = dataFolder.listFiles(File::isDirectory);

      if (planarRegionFolders == null || planarRegionFolders.length != 1)
         return false;

      return PlanarRegionFileTools.isPlanarRegionFile(planarRegionFolders[0]);
   }

   protected static String getDatasetNameFromResourceName(String datasetResourceName)
   {
      String[] resourcePath = datasetResourceName.split("/");
      return resourcePath[resourcePath.length - 1];
   }

   protected static String getPlanarRegionsDirectoryName(String datasetName)
   {
      String date = datasetName.substring(0, 15);
      return date + "_PlanarRegion";
   }

   protected static BufferedReader createBufferedReaderFromResource(Class<?> clazz, String expectedParametersResourceName)
   {
      InputStream parametersFile = clazz.getResourceAsStream(expectedParametersResourceName);
      if (parametersFile == null)
         throw new RuntimeException("Could not find the parmeter file: " + expectedParametersResourceName);

      InputStreamReader inputStreamReader = new InputStreamReader(parametersFile);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      return bufferedReader;
   }

   public static class VisibilityGraphsUnitTestDataset
   {
      private final String datasetName;
      private final String datasetResourceName;

      private int expectedPathSize;
      private Point3D start;
      private Point3D goal;
      private PlanarRegionsList planarRegionsList;

      public VisibilityGraphsUnitTestDataset(String datasetName, String datasetResourceName)
      {
         this.datasetName = datasetName;
         this.datasetResourceName = datasetResourceName;
      }

      public VisibilityGraphsUnitTestDataset(Class<?> clazz, String datasetResourceName)
      {
         this.datasetName = getDatasetNameFromResourceName(datasetResourceName);
         this.datasetResourceName = datasetResourceName;

         loadFromResource(clazz, datasetResourceName);
      }

      private void loadFromResource(Class<?> clazz, String datasetResourceName)
      {
         String expectedParametersResourceName = "/" + datasetResourceName + "/" + INPUTS_PARAMETERS_FILENAME;
         String expectedPlanarRegionsResourceName = "/" + datasetResourceName + "/" + getPlanarRegionsDirectoryName(datasetName);

         BufferedReader bufferedReader = createBufferedReaderFromResource(clazz, expectedParametersResourceName);
         try
         {
            bufferedReader.mark(10000);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }

         loadFields(bufferedReader);

         try
         {
            bufferedReader.close();
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }

         PlanarRegionsList planarRegionsList = PlanarRegionFileTools.importPlanarRegionData(clazz, expectedPlanarRegionsResourceName);
         setPlanarRegionsList(planarRegionsList);

         if (start == null)
            throw new RuntimeException("Could not load the start position. Data file: " + expectedParametersResourceName);
         if (goal == null)
            throw new RuntimeException("Could not load the goal position. Data file: " + expectedParametersResourceName);
         if (planarRegionsList == null)
            throw new RuntimeException("Could not load the planar regions. Data folder: " + expectedPlanarRegionsResourceName);
      }

      protected void loadFields(BufferedReader bufferedReader)
      {
         int expectedPathSize = parsePathSize(bufferedReader);
         Point3D start = parseStartField(bufferedReader);
         Point3D goal = parseGoalField(bufferedReader);

         setExpectedPathSize(expectedPathSize);
         setStart(start);
         setGoal(goal);
      }

      public void setExpectedPathSize(int expectedPathSize)
      {
         this.expectedPathSize = expectedPathSize;
      }

      public void setStart(Point3D start)
      {
         this.start = start;
      }

      private void setGoal(Point3D goal)
      {
         this.goal = goal;
      }

      public void setPlanarRegionsList(PlanarRegionsList planarRegionsList)
      {
         this.planarRegionsList = planarRegionsList;
      }

      public String getDatasetName()
      {
         return datasetResourceName;
      }

      public boolean hasExpectedPathSize()
      {
         return expectedPathSize > 0;
      }

      public int getExpectedPathSize()
      {
         return expectedPathSize;
      }

      public Point3D getStart()
      {
         return new Point3D(start);
      }

      public Point3D getGoal()
      {
         return new Point3D(goal);
      }

      public PlanarRegionsList getPlanarRegionsList()
      {
         return planarRegionsList;
      }

   }
}
