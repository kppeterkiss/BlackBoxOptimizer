
package optimizer.trial;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.ProgressMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DockerHandler {


   /* private static DockerClient getDockerClient() {
    /*


        final String localDockerHost = SystemUtils.IS_OS_WINDOWS ? "tcp://localhost:2375" : "unix:///var/run/docker.sock";

        final DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(localDockerHost)
                .build();

        return DockerClientBuilder
                .getInstance(config)
                .build();
    }*/

    final DockerClient dockerClient = DefaultDockerClient.fromEnv().build();

    public DockerHandler() throws DockerCertificateException {
    }

    /**
     * Builds the image with the given name and path.
     * @param resourcePath The path to the resources that should be included in the container.
     * @param imageName The imageId of the image to be created, or retrieved.
     * @return id of the container with the given imageId.
     * @throws InterruptedException
     * @throws DockerException
     * @throws IOException
     * @throws DockerCertificateException
     */
    public  String buildDockerImage(String resourcePath, String imageName) throws InterruptedException, DockerException, IOException, DockerCertificateException {


        final AtomicReference<String> imageIdFromMessage = new AtomicReference<>();
        final String returnedImageId = dockerClient.build(
                Paths.get(resourcePath), imageName, new ProgressHandler() {
                    @Override
                    public void progress(ProgressMessage message) throws DockerException {
                        final String imageId = message.buildImageId();
                        if (imageId != null) {
                            imageIdFromMessage.set(imageId);
                        }
                    }
                });
        return returnedImageId;
    }

    /**
     * Creates container from the given id.
     * @param imgId Id of the image to be instantiated.
     * @return
     * @throws DockerException
     * @throws InterruptedException
     */
    public String createContainer(String imgId) throws DockerException, InterruptedException {
        final ContainerConfig config = ContainerConfig.builder()
                .image(imgId)
                .cmd("sh", "-c", "while :; do sleep 1; done")
                .build();
        final ContainerCreation creation = dockerClient.createContainer(config, "trielcont");
        return creation.id();
    }
    public String createContainer(String imgId, String[] cmd) throws DockerException, InterruptedException {
        final ContainerConfig config = ContainerConfig.builder()
                .image(imgId)
                .cmd(cmd)
                .build();
        final ContainerCreation creation = dockerClient.createContainer(config, "trielcont");
        return creation.id();
    }



    Map<String, Container> imageNameContainerInstanceMap =new HashMap<>();

    public static void main(String[] args) throws InterruptedException, IOException, DockerCertificateException, DockerException {
        optimizer.trial.DockerHandler dh = new optimizer.trial.DockerHandler();
        //final DockerClient docker = DefaultDockerClient.fromEnv().build();
        // String imgId=dh.buildDockerImage("/Users/peterkiss/IdeaProjects/BBCom/repo/Rosenbrock","rosenbrock");
        //String  containerId = dh.createContainer(imgId);
        Container c = dh.getContainer("/Users/peterkiss/IdeaProjects/BBCom/repo/Rosenbrock","rosenbrock");
        c.start();
        System.out.println(c.execute( new String[]{"python" , "Rosenbrock.py", "20.0", "10.0"},null));
        //Container c = dh.getContainer("/Users/peterkiss/IdeaProjects/BBCom/repo/Rosenbrock","rosenbrock");
        //Container.execute1(dh,c.imageId,new String[]{"python" , "Rosenbrock.py", "20.0", "10.0"},null);
        //System.out.println(c.execute( new String[]{"python" , "Rosenbrock.py", "20.0", "10.0"},null));
        //c.stop();
        //c.remove();
        //final ContainerCreation creation = docker.createContainer(config, "trielcont");
        //final String id = creation.id();

        //docker.stopContainer("trielcont", 10); // kill after 10 seconds
        // final String execId = docker.execCreate("trielcont", new String[]{"python" , "Rosenbrock.py", "20.0", "10.0"}).id();
        //String res = "";
        //try (final LogStream stream = docker.execStart(execId)) {
        //    res =stream.readFully();
        //}
        //System.out.println(res);



    }

    /**
     * Creates the container from the image with the id given as parameter and wraps it into a {@link Container} object.
     * @param imageId
     * @return
     * @throws DockerException
     * @throws InterruptedException
     */
    private Container instantiateImage(String imageId) throws DockerException, InterruptedException {
        String containerId= this.createContainer(imageId);
        return new Container(this.dockerClient,containerId,imageId);
    }
    private Container instantiateImage(String imageId,String[] cmd) throws DockerException, InterruptedException {
        String containerId= this.createContainer(imageId,cmd);
        return new Container(this.dockerClient,containerId,imageId);
    }

    /**
     * REturns a container from an  image with the given name, and resource path.
     * @param resourcePath path to the folder with the Dockerfile.
     * @param imgName Name of the image to be created.
     * @return
     * @throws DockerException
     * @throws InterruptedException
     * @throws IOException
     * @throws DockerCertificateException
     */
    public Container getContainer(String resourcePath,String imgName) throws DockerException, InterruptedException, IOException, DockerCertificateException {
        //if there is a container already return that
        Container ret = imageNameContainerInstanceMap.get(imgName);
        if(ret != null)
            return ret;
        //otherwise if thew image exists find its id.
        List<Image> il = dockerClient.listImages();
        Optional<Image> i = il.stream().filter(image -> image.repoTags().stream().filter(tag -> tag.contains(imgName)).findFirst().isPresent()).findFirst();
        String imageId = null;
        if (!i.isPresent())
            //if no such image, we create one with the given path to the Dockerfile
            imageId = this.buildDockerImage(resourcePath, imgName);
        else imageId = i.get().id();
        //instantiating the image
        ret = instantiateImage(imageId);
        //register

        this.imageNameContainerInstanceMap.put(imgName, ret);
        //id of container
        return ret;

    }

    /**
     * Stops the container instantiated from the image with imgId.
     * @param imgId
     * @throws DockerException
     * @throws InterruptedException
     */
    public void stopContainer(String imgId) throws DockerException, InterruptedException {
        this.imageNameContainerInstanceMap.get(imgId).stop();
    }

    /**
     * Removes the container
     * @param imgId
     * @throws DockerException
     * @throws InterruptedException
     */
    public void removeContainer(String imgId) throws DockerException, InterruptedException {
        this.imageNameContainerInstanceMap.get(imgId).remove();
        this.imageNameContainerInstanceMap.remove(imgId);
    }
    public static class Container{
        DockerClient dockerClient;
        String containerId;
        String imageId;

        public Container(DockerClient dockerClient, String containerId, String ImageId) {
            this.dockerClient = dockerClient;
            this.containerId = containerId;
            this.imageId = ImageId;
        }

        public void start() throws DockerException, InterruptedException {
            dockerClient.startContainer(this.containerId);
        }

        // TODO: 2019. 01. 15. probably kills parallelism
        public synchronized String execute(String[] command, OutputStream os) throws DockerException, InterruptedException {
            String execId = dockerClient.execCreate(this.containerId, command, DockerClient.ExecCreateParam.attachStdout(),
                    DockerClient.ExecCreateParam.attachStderr()).id();

            //todo should be separated here
            try (final LogStream stream = dockerClient.execStart(execId)) {
                return stream.readFully();
            }
        }

        public static String execute1(DockerHandler dc,String ImgId,String[] command, OutputStream os,String path) throws DockerException, InterruptedException {
            Container c = dc.instantiateImage(ImgId,command);
            c.start();
            return "x";
        }
        public void stop() throws DockerException, InterruptedException {
            this.dockerClient.stopContainer(this.containerId,10);
        }

        public void remove() throws DockerException, InterruptedException {
            this.dockerClient.removeContainer(this.containerId);

        }
    }

}
