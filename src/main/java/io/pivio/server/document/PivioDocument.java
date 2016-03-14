package io.pivio.server.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "steckbrief", type = "steckbrief")
@Setting(settingPath = "settings.json")
@Mapping(mappingPath = "mapping.json")
public class PivioDocument {

  @Id
  private String id;

  private String type;
  private String name;
  private String serviceName;
  private String owner;
  private String description;

  public static class PivioDocumentBuild {

    private PivioDocument pivioDocument = new PivioDocument();

    public PivioDocumentBuild id(String id) {
      pivioDocument.setId(id);
      return this;
    }

    public PivioDocumentBuild type(String type) {
      pivioDocument.setType(type);
      return this;
    }

    public PivioDocumentBuild name(String name) {
      pivioDocument.setName(name);
      return this;
    }

    public PivioDocumentBuild serviceName(String serviceName) {
      pivioDocument.setServiceName(serviceName);
      return this;
    }

    public PivioDocumentBuild description(String description) {
      pivioDocument.setDescription(description);
      return this;
    }

    public PivioDocumentBuild owner(String team) {
      pivioDocument.setOwner(team);
      return this;
    }

    public PivioDocument build() {
      return pivioDocument;
    }
  }

  public PivioDocument(String id, String type, String name, String serviceName, String owner, String description) {
    this.id = id;
    this.type = type;
    this.name = name;
    this.serviceName = serviceName;
    this.owner = owner;
    this.description = description;
  }

  public static PivioDocumentBuild builder() {
    return new PivioDocumentBuild();
  }

  protected PivioDocument() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PivioDocument that = (PivioDocument) o;

    if (!description.equals(that.description)) {
      return false;
    }
    if (!id.equals(that.id)) {
      return false;
    }
    if (!name.equals(that.name)) {
      return false;
    }
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) {
      return false;
    }
    if (!owner.equals(that.owner)) {
      return false;
    }
    if (!type.equals(that.type)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + owner.hashCode();
    result = 31 * result + description.hashCode();
    return result;
  }
}
