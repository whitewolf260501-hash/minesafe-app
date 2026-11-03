from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    api_key: str = ""
    min_observations: int = 5
    max_observations: int = 10_000
    cors_origins: str = "http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000"
    model_config = SettingsConfigDict(env_prefix="MINESAFE_AI_", env_file=".env", extra="ignore")


settings = Settings()
