export class AuthenticationResponse {
  status: string;
  error: string;
  access_token: string;
  public_key: string;
  private_key_enc: string;
  private_key_salt: string;
}
