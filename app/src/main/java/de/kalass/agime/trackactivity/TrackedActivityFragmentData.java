package de.kalass.agime.trackactivity;

import com.google.common.base.Strings;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.List;

import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.customfield.ActivityCustomFieldEditorModel;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.timesuggestions.TimeSuggestions;
import de.kalass.android.common.util.StringUtil;

/**
* Created by klas on 21.01.14.
*/
final class TrackedActivityFragmentData {

    private static final class ProjectData {
        private Long id;
        private String name;
        private Integer colorCode;

        public void set(ProjectModel m) {
            if (m == null) {
                set(null, null, null);
            } else {
                set(m.getId(), m.getName(), m.getColorCode());
            }
        }

        public void set(Long id, String name, Integer colorCode) {
            this.id = id;
            this.name = name;
            this.colorCode = colorCode;
        }
    }

    private static final class CategoryData {
        private Long id;
        private String name;
        private Integer colorCode;

        public void set(Long id, String name, Integer colorCode) {
            this.id = id;
            this.name = name;
            this.colorCode = colorCode;
        }
    }

    private static final class ActivityTypeData {
        private Long id;
        private String name;


        public void set(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class TrackedActivityData {
        static final long ID_UNSAVED = -1;
        private long id = ID_UNSAVED;
        private LocalTime startTime;
        private LocalTime endTime;
        Long projectId;
        String details;
        Long activityTypeId;
        LocalDate date;
    }

    private final ProjectData project = new ProjectData();
    private final CategoryData category = new CategoryData();
    private final ActivityTypeData activityType = new ActivityTypeData();

    private final TrackedActivityData trackedActivity;

    private Integer newProjectColor;
    private TimeSuggestions timeSuggestions;
    final long id;
    final List<ActivityCustomFieldEditorModel> customFields;
    final List<RecurringDAO.Data> acquisitionTimes;
    final Long originalInsertDurationMillis;
    final Long originalUpdateDurationMillis;
    final Long originalUpdateCount;

    TrackedActivityFragmentData(
            Long entityId,
            List<ActivityCustomFieldEditorModel> customFields,
            List<RecurringDAO.Data> acquisitionTimes,
            int newProjectColor,
            Long originalInsertDurationMillis,
            Long originalUpdateDurationMillis,
            Long originalUpdateCount
    ) {
        this.newProjectColor = newProjectColor;
        this.originalInsertDurationMillis = originalInsertDurationMillis;
        this.originalUpdateDurationMillis = originalUpdateDurationMillis;
        this.originalUpdateCount = originalUpdateCount;
        this.id = entityId == null ? TrackedActivityData.ID_UNSAVED : entityId.longValue();
        this.customFields = customFields;
        this.acquisitionTimes = acquisitionTimes;
        this.trackedActivity = new TrackedActivityData();
    }

    public String getDetails() {
        return this.trackedActivity.details;
    }

    public void setDetails(String details) {
        this.trackedActivity.details = details;
    }

    public List<RecurringDAO.Data> getAcquisitionTimes() {
        return acquisitionTimes;
    }

    public void setSelectedProject(ProjectModel projectModel) {
        if (projectModel == null) {
            setSelectedProject(null, null, null);
        } else {
            setSelectedProject(projectModel.getId(), projectModel.getName(), projectModel.getColorCode());
        }
    }

    public void setSelectedProject(Long id, String name, Integer colorCode) {
        this.project.set(id, name, id == null && colorCode == null && !StringUtil.isTrimmedNullOrEmpty(name) ? newProjectColor : colorCode);
        this.trackedActivity.projectId = id;
    }

    public Long getProjectId() {
        return this.project.id;
    }

    public String getProjectName() {
        return this.project.name;
    }

    public Integer getProjectColorCode() {
        return this.project.colorCode;
    }

    public Integer getNewProjectColor() {
        return this.newProjectColor;
    }

    public void setSelectedActivityTypeAndCategory(ActivityTypeModel m) {
        if (m == null) {
            setSelectedActivityType(null, null);
            setSelectedCategory(null);
        } else {
            setSelectedActivityType(m.getId(), m.getName());
            setSelectedCategory(m.getCategoryModel());
        }
    }

    public void setSelectedActivityType(Long activityTypeId, String activityTypeName) {

        this.activityType.set(activityTypeId, activityTypeName);
        this.trackedActivity.activityTypeId = this.activityType.id;
    }

    public void setSelectedCategory(Long id, String name, Integer colorCode) {
        this.category.set(id, name, colorCode);
    }

    public void setSelectedCategory(CategoryModel m) {
        if (m == null) {
            setSelectedCategory(null, null, null);
        } else {
            setSelectedCategory(m.getId(), m.getName(), m.getColour());
        }
    }
    public Long getCategoryId() {
        return this.category.id;
    }

    public String getCategoryName() {
        return this.category.name;
    }

    public Integer getCategoryColorCode() {
        return this.category.colorCode;
    }


    public String getActivityTypeName() {
        return this.activityType.name;
    }

    public Long getActivityTypeId() {
        return this.activityType.id;
    }

    public TimeSuggestions getTimeSuggestions() {
        return timeSuggestions;
    }

    public void setTimes(LocalTime startTime, LocalTime endTime) {
        // make sure that automatically calculated start and endtime suggestions do not lead
        // to negative times
        trackedActivity.startTime = startTime;
        trackedActivity.endTime = endTime.isBefore(startTime) ? startTime : endTime;
    }

    public void setStartTime(LocalTime startTime) {
        if (startTime.isAfter(trackedActivity.endTime)) {
            setTimes(startTime, startTime);
        } else {
            trackedActivity.startTime = startTime;
        }
    }

    public void setEndTime(LocalTime endTime) {
        if (endTime.isBefore(trackedActivity.startTime)) {
            setTimes(endTime, endTime);
        } else {
            trackedActivity.endTime = endTime;
        }
    }


    /**
     * Required
     */
    public void setDate(LocalDate date, TimeSuggestions timeSuggestions) {
        if (trackedActivity.startTime == null) {
            trackedActivity.startTime = timeSuggestions.getStarttimeDefault(id).toLocalTime();
        }
        if (trackedActivity.endTime == null) {
            trackedActivity.endTime = timeSuggestions.getEndtimeDefault(id).toLocalTime();
        }
        trackedActivity.date = date;
        this.timeSuggestions = timeSuggestions;
    }

    public LocalDate getDate() {
        return trackedActivity.date;
    }

    public LocalTime getStartTime() {
        return trackedActivity.startTime;
    }

    public LocalTime getEndTime() {
        return trackedActivity.endTime;
    }

    public long getStartTimeMillis() {
        return trackedActivity.date.toDateTime(trackedActivity.startTime).getMillis();
    }

    public long getEndTimeMillis() {
        return trackedActivity.date.toDateTime(trackedActivity.endTime).getMillis();
    }
}
