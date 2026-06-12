import { Request, Response } from 'express';

export default function handler(req: Request, res: Response) {
  res.status(200).json({
    name: 'Omni API',
    status: 'online',
    version: '1.0.0'
  });
}
