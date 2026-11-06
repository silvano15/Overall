import dotenv from 'dotenv';
dotenv.config();

export const ENV = {
  PORT: process.env.PORT ? parseInt(process.env.PORT, 10) : 8080,
  JWT_SECRET: process.env.JWT_SECRET || 'dev-apenas-local-troque-em-producao-yz8eBFZTTbtzONEz7t2TWRvu0sL77CBBm2K0Zi0',
  JWT_EXPIRATION_HOURS: process.env.JWT_EXPIRATION_HOURS ? parseInt(process.env.JWT_EXPIRATION_HOURS, 10) : 720,
  TIMEZONE: process.env.APP_TIMEZONE || 'America/Recife',
};
