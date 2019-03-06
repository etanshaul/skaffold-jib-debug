package com.eshaul.gcpnext.demo;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Image;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import io.grpc.internal.IoUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

@RestController
public class HelloController {
    @RequestMapping("/java")
    public String index() {

//        String extractedText = "nothing";
//        try {
//            extractedText = extractText();
//        } catch (Exception e) {
//            extractedText = e.getMessage();
//            e.printStackTrace();
//        }
//        return extractedText;
        return "java";
    }

    @RequestMapping(value = "/image", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getImage() {
        FileInputStream image = null;
        byte[] imageBytes = null;
        try {
            image = new FileInputStream(extractText());
            imageBytes = IoUtils.toByteArray(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        byte[] image = imageService.getImage(id);
//        image.
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(imageBytes);
    }

    private File extractText() throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        StringBuilder result = new StringBuilder();


        ByteString imgBytes = ByteString.readFrom(getClass().getResourceAsStream("/images/k8s.png"));
        File newFile = Paths.get(new File(getClass().getResource("/images/k8s.png").getFile()).getParent(), "k8s-woo.png").toFile();

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        List<BoundingPoly> boundingPolies = Lists.newArrayList();
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
//                    return "failed";
                    return null;
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                // todo find the text who's bounding box is the largest > title
                // https://cloud.google.com/vision/docs/detecting-fulltext to extract blocks/paragraphs
                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    result.append(annotation.getDescription() + ":");
                    out.printf("Text: %s\n", annotation.getDescription());
                    out.printf("Position : %s\n", annotation.getBoundingPoly());
                    boundingPolies.add(annotation.getBoundingPoly());
                }
            }

//            String currentDir = new File(".").getAbsolutePath();
//            System.out.println("current path: -------  " + currentDir);
            BufferedImage bimg = ImageIO.read(new File(getClass().getResource("/images/k8s.png").getFile()));
            writeImageWithBoundingPolys(bimg, boundingPolies);
            boolean written = ImageIO.write(bimg, "png", newFile); //Paths.get("/Users/eshaul/k8s-new").toFile());//new File(getClass().getResource("/images/k8s-bounding.png").getFile()));

            System.out.println("written-------------------" + written +":" + newFile.getAbsolutePath());
        }

//        return result.toString();
        return newFile;
    }

    private void writeImageWithBoundingPolys(BufferedImage img, List<BoundingPoly> polys) throws IOException {
        Graphics2D gfx = img.createGraphics();
        gfx.setStroke(new BasicStroke(5));
        gfx.setColor(new Color(0x00ff00));

        for (BoundingPoly poly: polys) {
            Polygon jpoly = new Polygon();
            for (Vertex vertex: poly.getVerticesList()) {
                jpoly.addPoint(vertex.getX(), vertex.getY());
            }
            gfx.draw(jpoly);
        }

    }
}