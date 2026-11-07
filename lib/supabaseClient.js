import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://rsxttzenmxkuwrfrbkbv.supabase.co';
const supabaseKey = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY || 'sb_publishable_RkOx-OIQIHWS0kSmoj-n7A_ltEygh4V';

export const supabase = createClient(supabaseUrl, supabaseKey);

