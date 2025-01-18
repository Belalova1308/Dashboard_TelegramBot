package ru.project.tgbot.irishGeoSpringBot.services;


import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;
import ru.project.tgbot.irishGeoSpringBot.models.Tag;
import ru.project.tgbot.irishGeoSpringBot.models.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WriteFile {

    private final TaskService taskService;
    private final PeopleService peopleService;
    private final TagService tagService;

    public WriteFile(TaskService taskService, PeopleService peopleService, TagService tagService) {
        this.taskService = taskService;
        this.peopleService = peopleService;
        this.tagService = tagService;
    }

    public void writeWordFile(Long chatId, List<Task> tasks) {
        try {
            // Define the file path
            String folderPath = "C:/sendToTg";
            String fileName = "document.docx";

            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(folderPath + "/" + fileName);
            try {
                XWPFDocument document = new XWPFDocument();
                // Add a paragraph to the document
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                paragraph.setAlignment(ParagraphAlignment.CENTER);
                run.setText("Ваши задачи");
                run.setBold(true);
                run.setFontSize(30);
                run.setFontFamily("Times New Roman");

                Map<Tag, List<Task>> tasksByTag = taskService.showTasksGroupedByTag(chatId);
//                List<Task> tasks = taskService.showAllTasks(chatId);
                for (Tag tag : tasksByTag.keySet()) {

                    XWPFParagraph tagParagraph = document.createParagraph();
                    XWPFRun tagRun = tagParagraph.createRun();
                    tagRun.setText("🔖" + tag.getName());
                    tagRun.setBold(true);
                    tagRun.setFontSize(25);
                    tagRun.setFontFamily("Times New Roman");

                    for (Task task : tasksByTag.get(tag)){
                        StringBuilder oneTask= new StringBuilder();
                        XWPFParagraph paragraphTask = document.createParagraph();
                        XWPFRun runTask = paragraphTask.createRun();
                        paragraphTask.setIndentationLeft(500);
                        oneTask.append(task.getStatus().equals("Сделано") ? "✅ " : "⭕ ");
                        oneTask.append(task.getTitle());
                        runTask.setText(String.valueOf(oneTask));
                        runTask.setFontSize(20);
                        runTask.setFontFamily("Times New Roman");
                    }
                }


                // Write the document to the file
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    document.write(fos);
                }
            } catch (Exception e){
                throw new RuntimeException("Error creating document", e);
            }

            System.out.println("File created successfully at: " + file.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Error creating the Word file", e);
        }
    }

}
