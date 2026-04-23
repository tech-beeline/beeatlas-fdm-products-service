ALTER TABLE product
    ADD COLUMN description VARCHAR(255) NULL,
ADD COLUMN git_url VARCHAR(255) NULL,
ADD COLUMN structurizr_workspace_name VARCHAR(255) NULL,
ADD COLUMN structurizr_api_key VARCHAR(255) NULL,
ADD COLUMN structurizr_api_secret VARCHAR(255) NULL,
ADD COLUMN structurizr_api_url VARCHAR(255) NULL;